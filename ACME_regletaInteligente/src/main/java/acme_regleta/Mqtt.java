package acme_regleta;

import java.util.ArrayList;
//import java.util.Calendar;
import java.util.List;
//import java.util.Timer;
//import java.util.TimerTask;
import java.util.stream.Stream;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
//import io.vertx.core.buffer.Buffer;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;
import io.vertx.mqtt.MqttEndpoint;
import io.vertx.mqtt.MqttServer;
import io.vertx.mqtt.MqttTopicSubscription;
import io.vertx.mqtt.messages.MqttPublishMessage;

public class Mqtt extends AbstractVerticle{
	
	static int MQTT_PORT = 1883;
	static String canal_1 = "main_topic";

	private static Multimap<String, MqttEndpoint> clientTopics;
	
	/**
	 * - Topic principal del proyecto: AT_LEAST_ONCE.
	 * - Creado variable global para la introduccion de la IP del servidor: MQTT_PORT.
	 */
	
	public void start(Future<Void> startFuture) {
		clientTopics = HashMultimap.create();
		// Configuramos el servidor MQTT
		MqttServer mqttServer = MqttServer.create(vertx);
		init(mqttServer);
		
		/*
		 * Ahora creamos un segundo cliente, al que se supone deben llegar todos los
		 * mensajes que el cliente 1, desplegado anteriormente, publique en el topic
		 * "main_topic". Este era el punto en el que el proyecto anterior fallaba, debido a
		 * que no existía ningún broker que se encargara de realizar el envío desde el
		 * servidor al resto de clientes.
		 */
		
		MqttClient mqttClient2 = MqttClient.create(vertx, new MqttClientOptions().setAutoKeepAlive(true));
		mqttClient2.connect(MQTT_PORT, "localhost", s -> {

			/*
			 * Al igual que antes, este cliente se suscribe al topic_2 para poder recibir
			 * los mensajes que el cliente 1 envíe a través de MQTT.
			 */
			mqttClient2.subscribe(canal_1, MqttQoS.AT_LEAST_ONCE.value(), handler -> {
				if (handler.succeeded()) {
					/*
					 * En este punto, el cliente 2 también está suscrito al servidor, por lo que ya
					 * podrá empezar a recibir los mensajes publicados en el topic.
					 */
					
					/*
					 * Además de suscribirnos al servidor, registraremos un manejador para
					 * interceptar los mensajes que lleguen a nuestro cliente. De manera que el
					 * proceso será el siguiente: El cliente 1 envía un mensaje al servidor -> el
					 * servidor lo recibe y busca los clientes suscritos al topic -> el servidor
					 * reenvía el mensaje a esos clientes -> los clientes (en este caso el cliente
					 * 2) recibe el mensaje y lo proceso si fuera necesario.
					 */
					
					mqttClient2.publishHandler(new Handler<MqttPublishMessage>() {
						@Override
						public void handle(MqttPublishMessage arg0) {
							/*
							 * Si se ejecuta este código es que el cliente 2 ha recibido un mensaje
							 * publicado en algún topic al que estaba suscrito (en este caso, al topic_2).
							 */
								
							//TODO: Procesamiento del mensaje.
							
							System.out.println("Mensaje recibido por el cliente 2: " + arg0.payload().toString());
						}
					});
				}
			});
		});
	}

	/**
	 * Método encargado de inicializar el servidor y ajustar todos los manejadores
	 * 
	 * @param mqttServer
	 */
	private static void init(MqttServer mqttServer) {
		mqttServer.endpointHandler(endpoint -> {
			/*
			 * Si se ejecuta este código es que un cliente se ha suscrito al servidor MQTT
			 * para algún topic.
			 */
			System.out.println("Nuevo cliente MQTT [" + endpoint.clientIdentifier()
					+ "] solicitando suscribirse [Id de sesión: " + endpoint.isCleanSession() + "]");
			/*
			 * Indicamos al cliente que se ha contectado al servidor MQTT y que no tenía
			 * sesión previamente creada (parámetro false)
			 */
			endpoint.accept(false);

			/*
			 * Handler para gestionar las suscripciones a un determinado topic. Aquí
			 * registraremos el cliente para poder reenviar todos los mensajes que se
			 * publicen en el topic al que se ha suscrito.
			 */
			handleSubscription(endpoint);

			/*
			 * Handler para gestionar las desuscripciones de un determinado topic. Haremos
			 * lo contrario que el punto anterior para eliminar al cliente de la lista de
			 * clientes registrados en el topic. De este modo, no seguirá recibiendo
			 * mensajes en este topic.
			 */
			handleUnsubscription(endpoint);

			/*
			 * Este handler será llamado cuando se publique un mensaje por parte del cliente
			 * en algún topic creado en el servidor MQTT. En esta función obtendremos todos
			 * los clientes suscritos a este topic y reenviaremos el mensaje a cada uno de
			 * ellos. Esta es la tarea principal del broken MQTT. En este caso hemos
			 * implementado un broker muy muy sencillo. Para gestionar QoS, asegurar la
			 * entrega, guardar los mensajes en una BBDD para después entregarlos, guardar
			 * los clientes en caso de caída del servidor, etc. debemos recurrir a un código
			 * más elaborado o usar una solución existente como por ejemplo Mosquitto.
			 */
			publishHandler(endpoint);

			/*
			 * Handler encargado de gestionar las desconexiones de los clientes al servidor.
			 * En este caso eliminaremos al cliente de todos los topics a los que estuviera
			 * suscrito.
			 */
			handleClientDisconnect(endpoint);
			
		}).listen(ar -> {
			if (ar.succeeded()) {
				System.out.println("MQTT server está a la escucha por el puerto " + ar.result().actualPort());
			} else {
				System.out.println("Error desplegando el MQTT server");
				ar.cause().printStackTrace();
			}
		});
	}

	/**
	 * Método encargado de gestionar las suscripciones de los clientes a los
	 * diferentes topics. En este método registrará el cliente asociado al topic
	 * al que se suscribe
	 * 
	 * @param endpoint
	 */
	private static void handleSubscription(MqttEndpoint endpoint) {
		endpoint.subscribeHandler(subscribe -> {
			// Los niveles de QoS permiten saber el tipo de entrega que se realizará:
			// - AT_LEAST_ONCE: Se asegura que los mensajes llegan a los clientes, pero no
			// que se haga una única vez (pueden llegar duplicados)
			// - EXACTLY_ONCE: Se asegura que los mensajes llegan a los clientes una única
			// vez (mecanismo más costoso)
			// - AT_MOST_ONCE: No se asegura que el mensaje llegue al cliente, por lo que no
			// es necesario ACK por parte de éste
			List<MqttQoS> grantedQosLevels = new ArrayList<>();
			for (MqttTopicSubscription s : subscribe.topicSubscriptions()) {
				System.out.println("Suscripción al topic " + s.topicName() + " con QoS " + s.qualityOfService());
				grantedQosLevels.add(s.qualityOfService());

				// Añadimos al cliente en la lista de clientes suscritos al topic
				clientTopics.put(s.topicName(), endpoint);
			}

			/*
			 * Enviamos el ACK al cliente de que se ha suscrito al topic con los niveles de
			 * QoS indicados
			 */
			endpoint.subscribeAcknowledge(subscribe.messageId(), grantedQosLevels);
		});
	}

	/**
	 * Método encargado de eliminar la suscripción de un cliente a un topic. En este
	 * método eliminará al cliente de la lista de clientes suscritos a ese topic.
	 * 
	 * @param endpoint
	 */
	private static void handleUnsubscription(MqttEndpoint endpoint) {
		endpoint.unsubscribeHandler(unsubscribe -> {
			for (String t : unsubscribe.topics()) {
				// Eliminos al cliente de la lista de clientes suscritos al topic
				clientTopics.remove(t, endpoint);
				System.out.println("Eliminada la suscripción del topic " + t);
			}
			// Informamos al cliente que la desuscripción se ha realizado
			endpoint.unsubscribeAcknowledge(unsubscribe.messageId());
		});
	}

	/**
	 * Manejador encargado de notificar y procesar la desconexión de los clientes.
	 * 
	 * @param endpoint
	 */
	private static void handleClientDisconnect(MqttEndpoint endpoint) {
		endpoint.disconnectHandler(h -> {
			// Eliminamos al cliente de todos los topics a los que estaba suscritos
			Stream.of(clientTopics.keySet()).filter(e -> clientTopics.containsEntry(e, endpoint))
					.forEach(s -> clientTopics.remove(s, endpoint));
			System.out.println("El cliente remoto se ha desconectado [" + endpoint.clientIdentifier() + "]");
		});
	}

	/**
	 * Manejador encargado de interceptar los envíos de mensajes de los diferentes
	 * clientes. Este método deberá procesar el mensaje, identificar los clientes
	 * suscritos al topic donde se publica dicho mensaje y enviar el mensaje a cada
	 * uno de esos clientes.
	 * 
	 * @param endpoint
	 */
	private static void publishHandler(MqttEndpoint endpoint) {
		endpoint.publishHandler(message -> {
			/*
			 * Suscribimos un handler cuando se solicite una publicación de un mensaje en un
			 * topic
			 */
			handleMessage(message, endpoint);
		}).publishReleaseHandler(messageId -> {
			/*
			 * Suscribimos un handler cuando haya finalizado la publicación del mensaje en
			 * el topic
			 */
			endpoint.publishComplete(messageId);
		});
	}

	/**
	 * Método de utilidad para la gestión de los mensajes salientes.
	 * 
	 * @param message
	 * @param endpoint
	 */
	private static void handleMessage(MqttPublishMessage message, MqttEndpoint endpoint) {
		System.out.println("Mensaje publicado por el cliente " + endpoint.clientIdentifier() + " en el topic "
				+ message.topicName());
		System.out.println("    Contenido del mensaje: " + message.payload().toString());

		/*
		 * Obtenemos todos los clientes suscritos a ese topic (exceptuando el cliente
		 * que envía el mensaje) para así poder reenviar el mensaje a cada uno de ellos.
		 * Es aquí donde nuestro código realiza las funciones de un broken MQTT
		 */
		System.out.println("Origen: " + endpoint.clientIdentifier());
		for (MqttEndpoint client : clientTopics.get(message.topicName())) {
			System.out.println("Destino: " + client.clientIdentifier());
			if (!client.clientIdentifier().equals(endpoint.clientIdentifier()))
				try {
					client.publish(message.topicName(), message.payload(), message.qosLevel(), message.isDup(),
							message.isRetain()).publishReleaseHandler(idHandler -> {
								client.publishComplete(idHandler);
							});
				} catch (Exception e) {
					System.out.println("Error, no se pudo enviar mensaje.");
				}
		}

		if (message.qosLevel() == MqttQoS.AT_LEAST_ONCE) {
			String topicName = message.topicName();
			switch (topicName) {
			/*
			 * Se podría hacer algo con el mensaje como, por ejemplo, almacenar un registro
			 * en la base de datos
			 */
			case canal_1:
				System.out.println("HOLA");
				break;
			default:
				break;
			}
			// Envía el ACK al cliente de que el mensaje ha sido publicado
			endpoint.publishAcknowledge(message.messageId());
		} else if (message.qosLevel() == MqttQoS.EXACTLY_ONCE) {
			/*
			 * Envía el ACK al cliente de que el mensaje ha sido publicado y cierra el canal
			 * para este mensaje. Así se evita que los mensajes se publiquen por duplicado
			 * (QoS)
			 */
			
			endpoint.publishRelease(message.messageId());
		}
	}

}
package acme_regleta;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
//import io.vertx.core.http.HttpServer;

public class Main extends AbstractVerticle { // Es un verticle

	public void start(Future<Void> startFuture) {	//Despliega la funcionalidad de verticle
		//Vertx nos da la posibilidad de desplegar y replegar(undeployVerticle) todos los verticle que queramos.
//		vertx.deployVerticle(new CommSender());	
//		vertx.deployVerticle(new CommReceiver());
//		vertx.deployVerticle(new CommReceiverBroadcast());
		//USO DE JSON:-> vertx.deployVerticle(new JsonExamples());
		
		//Servidor TCP
//		vertx.deployVerticle(new TCPClient());
//		vertx.deployVerticle(new TCPServer());
		
		//API RES
		//vertx.deployVerticle(new ResServer());
		
		//API RES modificado - Proyecto
		vertx.deployVerticle(new ResServerRegleta());
		
		//SERVIDOR TCP PARA USO DE API REST:->
		
		//MQTT
		//vertx.deployVerticle(new Mqtt());
		
		/*
		vertx
			.createHttpServer()
								//handler nos permite definir que es lo que queremos que haga el vertx
				.requestHandler(request->{	//request es el parametro que le pasamos
					request.response()
						.end("<b> Hola mundo </b>");	//Respuesta a la peticion
				}).listen(8081, status -> {
					//Nos sirve para ver el estado del listen. Se ve en tres momentos : Despliegue, alguien se suscribe, se cierra.
					if(status.succeeded()) {
						System.out.println("Servidos HTTP desplegado");
						startFuture.succeeded(); //Le comentamos que se ha completado (completed()) y se ha lanzado
					}else {
						System.out.println("Error en el despliegue. " + 
												status.cause().getMessage()); //Nos devuelve el mensaje de error.
						startFuture.fail(status.cause());	//Le indicamos al servidor el error que ha sucedido para propagar el mensaje.
					}
				});
		
		*/
		
		
	}


}
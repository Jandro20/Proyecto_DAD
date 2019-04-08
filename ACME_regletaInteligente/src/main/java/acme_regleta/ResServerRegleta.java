package acme_regleta;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

import acme_regleta.entities.Usuario;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.asyncsql.AsyncSQLClient;
import io.vertx.ext.asyncsql.MySQLClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * Despliegue de una API Rest que se conecta a una db
 * @param <Usuario>
 * 
 */

public class ResServerRegleta extends AbstractVerticle{
	
	//Nos guarda la informacion del cliente una vez que nos conectamos en vez de guardarla por cada peticion
	private AsyncSQLClient mySQLClient;   
	
	
	public void start(Future<Void> startFuture) {
		
		JsonObject config = new JsonObject()
								.put("host", "localhost")	//Donde se encuentra el host -- nombre del dominio
									.put("username", "root")	//Nombre usuario
										.put("password", "root")	//Contraseña usuario
											.put("database", "DAD_ACME")	//Nombre del esquema en MySQLWorkbench
												.put("port", 3306);	//Puerto en el que se desplegó la base de datos.
		
		
		//Le pasamos el vertx y el archivo de configuracion creado anteriormente
		mySQLClient = MySQLClient.createShared(vertx, config);
		
		
		//Instanciamos un router:
		Router router = Router.router(vertx);
		
		//Creamos servidor HTTP e interceptamos las peticiones, delegando todo el enrutado en el router creado.
		vertx.createHttpServer()
			.requestHandler(router)		//Desplegamos el manejador de peticiones. Devuelve un httpResponse.
				.listen(8090, result ->{
					//Comprobamos si el servidor se ha desplegado correctamente.
					if(result.succeeded()) {
						System.out.println("Servidor desplegado correctamente");
					}else {
						System.out.println("Error en el despliegue");
					}
				});
		
		//Lanzamos el router
		router.route().handler(BodyHandler.create());	//Crea un cuerpo de peticiones y despliega el router.
		
		//A continuacion definimos las funciones que vamos a usar
//		router.get("")	//Definimos un get de API REST, donde pasamos por parametro la ruta para esa petición
		//Para el uso de variables se le añade -> : <- para decirle a vertx que no es texto.
		//Le decimos que para cuando reciba una peticion por esa ruta, la encamine por el handler que voy a definir a continuacion
		router.get("/usr/:usr/info")
				.handler(this::handlerUsr);
		router.get("/usr/:usr/dispositivo/:dispo/info")
				.handler(this::handlerDisp);
		router.get("/usr/:usr/dispositivo/:dispo/enchufe/:ench")
				.handler(this::handlerEnch);
		router.put("/usr/:usr/dispositivo/:dispo/enchufe/:ench/estado")
				.handler(this::handlerEstado);
		router.put("/usr/:usr/dispositivo/:dispo/enchufe/:ench/historico")
				.handler(this::handlerHistorico);
		//Al pasarle una ruta con mas de un parametro, si coge el ultimo, descarta el resto
		
	}
	
	private void handlerUsr(RoutingContext routingContext) {
		//Tengo el usuario que quiero conocer la info
		String paramStr = routingContext.pathParam("usr"); 
		
				//Creo una conexion con la ddbb
		mySQLClient.getConnection(connection ->{
			if(connection.succeeded()) {
				connection.result().query("SELECT * FROM usuario WHERE aliasUsuario = '" + paramStr +"'", result ->{		//result -> resultado de la conexion
				if(result.succeeded()) {											
					//Procesamiento de datos
					Gson gson = new Gson();
					List<Usuario> usuario= new ArrayList<>();
					
					//Iteramos cada objeto, lo convertimos a Usuario y lo añadimos a la lista.
					for (JsonObject json : result.result().getRows()) {
						usuario.add(gson.fromJson(json.encode(), Usuario.class));
						//Convertimos de jsonObject a object.
					}
					
					routingContext.response().end(gson.toJson(usuario));
					
					
				}else {
					System.out.println(result.cause().getMessage());
					routingContext
						.response()
							.setStatusCode(400)		//Le indicamos al cliente que ha habido un error 400
								.end();
				}
				});
				
			}else {
				System.out.println(connection.cause().getMessage());
			}
		});

	}
	
	private void handlerDisp(RoutingContext routingContext) {
		//Tengo el dispositivo que quiero conocer la info
				String paramUs = routingContext.pathParam("usr");
				String paramDis = routingContext.pathParam("dispo"); 
				
				//Creo una conexion con la ddbb
						mySQLClient.getConnection(connection ->{
							if(connection.succeeded()) {
								//Realizo la consulta a la base de datos.
								/*
								 * Dame la informacion del dispositivo x que tiene como usuario asignado el y.
								 * Posible: 
								 * select if
										((select idDispositivo from dad_acme.relacionuserdisp where idUsuario 
												= ANY (select idUsuario from dad_acme.usuario where aliasUsuario = 'ale'))
									        , 0
									        , 1);
								 */
								connection.result().query(
										"SELECT * from dad_acme.dispositivo where aliasDisp = "
												+ "'" + paramDis + "' "
													+ "= ANY (select idDispositivo from dad_acme.relacionuserdisp where aliasUsuario = '"+paramUs+"' );", result ->{		//result -> resultado de la conexion
									if(result.succeeded()) {								
										routingContext
											.response()
												.end(result.result().toJson().encodePrettily());	//Enviamos a nuestro cliente el resultado de la consulta.
										
									}else {
										System.out.println(result.cause().getMessage());
										routingContext
											.response()
												.setStatusCode(400)		//Le indicamos al cliente que ha habido un error 400
													.end();
									}
								});
								
							}else {
								System.out.println(connection.cause().getMessage());
							}
						});
	}
	
	private void handlerEnch(RoutingContext routingContext) {
		
	}
	
	private void handlerEstado(RoutingContext routingContext) {
		
	}
	
	private void handlerHistorico(RoutingContext routingContext) {
		
	}
	
	
	
	//Implementacion de la funcion. Necesita una estructura-> No devuelve nada y recibe por parametro un RoutingContext
	
	/*
	 * CASOS DE EJEMPLO
	 * 
	 * 
	 * private void handlerProduct(RoutingContext routingContext) { //Obtenemos el
	 * valor del parametro productID String paramStr =
	 * routingContext.pathParam("productID"); //Devuelve siempre como string (si lo
	 * necesitamos, lo parseamos) int paramInt = Integer.parseInt(paramStr);
	 * //Cuidado con las funciones parse, pasarlo mejor con un try/catch
	 * 
	 * JsonObject jsonObject = new JsonObject(); jsonObject.put("serial",
	 * "aisubfafs") .put("id", paramInt) .put("name", "TV Samsung");
	 * 
	 * routingContext.response() //Respuesta .putHeader("content-type",
	 * "application/json") .end(jsonObject.encode());
	 * 
	 * 
	 * }
	 * 
	 * private void handlerProductProperty(RoutingContext routingContext) {
	 * //Obtenemos el valor del parametro productID String paramStr =
	 * routingContext.pathParam("productID"); //Devuelve siempre como string (si lo
	 * necesitamos, lo parseamos) int paramInt = Integer.parseInt(paramStr);
	 * //Cuidado con las funciones parse, pasarlo mejor con un try/catch
	 * 
	 * JsonObject jsonObject = routingContext.getBodyAsJson(); //obtenemos el valor
	 * que nos pasa el cliente, ya que es un put
	 * 
	 * //Faltaría implementacion con base de datos.
	 * 
	 * routingContext.response() //Respuesta .putHeader("content-type",
	 * "application/json") .end(jsonObject.encode()); //Respondemos con lo mismo que
	 * nos ha enviado el cliente.
	 * 
	 * }
	 * 
	 * private void handlerAllSensors(RoutingContext routingContext) {
	 * 
	 * //Creo una conexion con la ddbb mySQLClient.getConnection(connection ->{
	 * if(connection.succeeded()) { // connection.result().query(sql, resultHandler)
	 * // connection.result().commit(handler) // connection.result().close();
	 * 
	 * connection.result().query("SELECT * FROM sensor", result ->{ //result ->
	 * resultado de la conexion if(result.succeeded()) { //
	 * result.result().getColumnNames(); // result.result().getNext(); //Nos sirve
	 * para iterar de resultado en resultado. // result.result().getNumColumns(); //
	 * result.result().getNumRows(); // result.result().getResults(); //Nos devuelve
	 * una lista de JSON Object, que podriamos recorrerlo con java8. //
	 * result.result().toJson().encodePrettily(); //Obtenemos el resultado completo
	 * en formato JSON
	 * 
	 * routingContext .response() .end(result.result().toJson().encodePrettily());
	 * //Enviamos a nuestro cliente el resultado de la consulta.
	 * 
	 * }else { System.out.println(result.cause().getMessage()); //Aqui le tenemos
	 * que mandar una respuesta al cliente. routingContext .response()
	 * .setStatusCode(400) //Le indicamos al cliente que ha habido un error 400
	 * .end(); } });
	 * 
	 * }else { System.out.println(connection.cause().getMessage()); } });
	 * 
	 * 
	 * 
	 * //Comprobar el estado de la conexion //Lanzar la consulta a la ddbb }
	 */

}

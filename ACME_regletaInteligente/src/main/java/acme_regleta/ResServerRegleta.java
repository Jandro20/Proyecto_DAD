package acme_regleta;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

import acme_regleta.entities.Historico;
import acme_regleta.entities.Estado;
import acme_regleta.entities.Enchufe;
import acme_regleta.entities.Dispositivo;
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
		router.get("/usr/:usr/dispositivo/:dispo/enchufe/:ench/info")
				.handler(this::handlerEnch);
		router.get("/usr/:usr/dispositivo/:dispo/enchufe/:ench/estado")
				.handler(this::handlerEstado);
		router.get("/usr/:usr/dispositivo/:dispo/enchufe/:ench/historico")
				.handler(this::handlerHistorico);
		
	}
	
	private void handlerUsr(RoutingContext routingContext) {
		//Consigo el parametro que me interesa de la url
		String paramStr = routingContext.pathParam("usr"); 
		
		//Creo la conexion con la base de datos
		mySQLClient.getConnection(connection ->{
			if(connection.succeeded()) {
				//Realizo la consulta a la base de datos
				connection.result().query("SELECT * FROM usuario WHERE aliasUsuario = '" + paramStr +"'", result ->{ 
				//Resultado de la peticion
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
		//Obtengo los parametros que necesito de la base de datos
		String paramUs = routingContext.pathParam("usr");
		String paramDis = routingContext.pathParam("dispo"); 
				
		//Creo una conexion con la ddbb
		mySQLClient.getConnection(connection ->{
			if(connection.succeeded()) {
				connection.result().query(
						//Realizo la peticion a la base de datos
						"SELECT dispositivo.idDispositivo, aliasDisp " + 
						"FROM dispositivo INNER JOIN relacionuserdisp ON dispositivo.idDispositivo = relacionuserdisp.idDispositivo" + 
						"				  INNER JOIN usuario ON relacionuserdisp.idUsuario = usuario.idUsuario"
						+ "			 where usuario.aliasUsuario = '"+ paramUs +"' and aliasDisp = '"+ paramDis +"';", result ->{		//result -> resultado de la conexion
							if(result.succeeded()) {								
								//Procesamiento de datos recibidos
								Gson gson = new Gson();
								List<Dispositivo> dispositivo= new ArrayList<>();
								
								//Iteramos cada objeto, lo convertimos a Usuario y lo añadimos a la lista.
								for (JsonObject json : result.result().getRows()) {
									dispositivo.add(gson.fromJson(json.encode(), Dispositivo.class));
									//Convertimos de jsonObject a object.
								}
										
								routingContext.response().end(gson.toJson(dispositivo));
								
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
		//Obtengo los parametros de la url que necesito
		String paramUs = routingContext.pathParam("usr");
		String paramDis = routingContext.pathParam("dispo");
		String paramEnch = routingContext.pathParam("ench");
		
		//Creo una conexion con la ddbb
		mySQLClient.getConnection(connection ->{
			if(connection.succeeded()) {
				connection.result().query(
						//Realizo la peticion a la base de datos
						"SELECT enchufe.idEnchufe, enchufe.aliasEnchufe" + 
						"	FROM enchufe where enchufe.aliasEnchufe = '"+ paramEnch +"' and enchufe.idDispositivo = (SELECT dispositivo.idDispositivo" + 
						"		FROM dispositivo INNER JOIN relacionuserdisp ON dispositivo.idDispositivo = relacionuserdisp.idDispositivo" + 
						"						 INNER JOIN usuario ON relacionuserdisp.idUsuario = usuario.idUsuario "
						+ "								where usuario.aliasUsuario = '"+ paramUs + "' and aliasDisp = '" + paramDis + "');", result ->{		//result -> resultado de la conexion
							if(result.succeeded()) {								
								//Procesamiento de datos
								Gson gson = new Gson();
								List<Enchufe> enchufe= new ArrayList<>();
								
								//Iteramos cada objeto, lo convertimos a Usuario y lo añadimos a la lista.
								for (JsonObject json : result.result().getRows()) {
									enchufe.add(gson.fromJson(json.encode(), Enchufe.class));
									//Convertimos de jsonObject a object.
								}
								
								routingContext.response().end(gson.toJson(enchufe));
								
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
	
	private void handlerEstado(RoutingContext routingContext) {
		//Obtengo los parámetros que necesito de la url
		String paramUs = routingContext.pathParam("usr");
		String paramDis = routingContext.pathParam("dispo");
		String paramEnch = routingContext.pathParam("ench");
		
		//Creo una conexion con la ddbb
		mySQLClient.getConnection(connection ->{
			if(connection.succeeded()) {
				connection.result().query(
						//Realizo la peticion a la base de datos
						"SELECT *" + 
						"	FROM estado where estado.idEnchufe = any (select enchufe.idEnchufe" + 
						"		FROM enchufe where enchufe.aliasEnchufe = '"+ paramEnch + "' and enchufe.idDispositivo = (SELECT dispositivo.idDispositivo " + 
						"			FROM dispositivo INNER JOIN relacionuserdisp ON dispositivo.idDispositivo = relacionuserdisp.idDispositivo " + 
						"							 INNER JOIN usuario ON relacionuserdisp.idUsuario = usuario.idUsuario "
						+ "								where usuario.aliasUsuario = '"+ paramUs + "' and aliasDisp = '"+ paramDis + "'));", result ->{		//result -> resultado de la conexion
							if(result.succeeded()) {								
								//Procesamiento de datos
								Gson gson = new Gson();
								List<Estado> estado= new ArrayList<>();
								
								//Iteramos cada objeto, lo convertimos a Usuario y lo añadimos a la lista.
								for (JsonObject json : result.result().getRows()) {
									estado.add(gson.fromJson(json.encode(), Estado.class));
									//Convertimos de jsonObject a object.
								}
								
								routingContext.response().end(gson.toJson(estado));
								
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
	
	private void handlerHistorico(RoutingContext routingContext) {
		//Obtengo los parametros que necesito de la url
		String paramUs = routingContext.pathParam("usr");
		String paramDis = routingContext.pathParam("dispo");
		String paramEnch = routingContext.pathParam("ench");
				
		mySQLClient.getConnection(connection ->{
			if(connection.succeeded()) {
				//Realizo la consulta a la base de datos.
				connection.result().query(
				//Realizo la peticion a la base de datos
				"SELECT idhistorico, consumo, fecha" + 
				"	FROM historico where historico.idEnchufe = any (select enchufe.idEnchufe" + 
				"		FROM enchufe where enchufe.aliasEnchufe = '" + paramEnch + "' and enchufe.idDispositivo = (SELECT dispositivo.idDispositivo" + 
				"		FROM dispositivo INNER JOIN relacionuserdisp ON dispositivo.idDispositivo = relacionuserdisp.idDispositivo" + 
				"						 INNER JOIN usuario ON relacionuserdisp.idUsuario = usuario.idUsuario "
				+ "									where usuario.aliasUsuario = '" + paramUs + "' and aliasDisp = '"+ paramDis +"'));", result ->{		//result -> resultado de la conexion						
				
					if(result.succeeded()) {								
						//Procesamiento de datos
						Gson gson = new Gson();
						List<Historico> historico= new ArrayList<>();
						
						//Iteramos cada objeto, lo convertimos a Usuario y lo añadimos a la lista.
						for (JsonObject json : result.result().getRows()) {
							historico.add(gson.fromJson(json.encode(), Historico.class));
							//Convertimos de jsonObject a object.
						}
			
						routingContext.response().end(gson.toJson(historico));
										
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
	
}
	
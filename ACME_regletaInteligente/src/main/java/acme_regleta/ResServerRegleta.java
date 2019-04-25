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
		
		//router.get("")	//Definimos un get de API REST, donde pasamos por parametro la ruta para esa petición
		//Para el uso de variables se le añade -> : <- para decirle a vertx que no es texto.
		//Le decimos que para cuando reciba una peticion por esa ruta, la encamine por el handler que voy a definir a continuacion
		
		router.get("/usr/:usr/info")
				.handler(this::handlerUsr);
		router.get("/usr/:usr/dispositivo/:dispo/info")
				.handler(this::handlerDisp);
		router.get("/usr/:usr/dispositivo/all")
				.handler(this::handlerAllDisp);
		router.get("/usr/:usr/dispositivo/:dispo/enchufe/all")
				.handler(this::handlerAllEnch);
		router.get("/usr/:usr/dispositivo/:dispo/enchufe/:ench/info")
				.handler(this::handlerEnch);
		router.get("/usr/:usr/dispositivo/:dispo/enchufe/:ench/estado")
				.handler(this::handlerEstado);
		router.get("/usr/:usr/dispositivo/:dispo/enchufe/:ench/historico")
				.handler(this::handlerHistorico);
		
		/*
		 * Informacion de las URL para las peticiones GET:
		 	- /usr/:usr/info : Nos devuelve la informacion del usuario usr
		 	- /usr/:usr/dispositivo/:dispo/info : Nos devuelve la informacion del dispositivo dispo si está asociado al usuario usr
		 	- /usr/:usr/dispositivo/all : Nos devuelve la informacion de todos los dispositivos asociados al usuario usr
		 	- /usr/:usr/dispositivo/:dispo/enchufe/all : Nos devuelve la informacion de todos los enchufes del dispositivo dispo asociados al usuario usr
		 	- /usr/:usr/dispositivo/:dispo/enchufe/:ench/info : Nos devuelve la informacion del enchufe ench del dispositivo dispo asociados al usuario usr
		 	- /usr/:usr/dispositivo/:dispo/enchufe/:ench/estado : Nos devuelve el estado del enchufe ench del dispositivo dispo asociados al usuario usr
		 	- /usr/:usr/dispositivo/:dispo/enchufe/:ench/historico : Nos devuelve el historico del enchufe ench del dispositivo dispo asociados al usuario usr
		 */
		
		// Peticiones PUT:
		
		router.put("/put/usr")	
				.handler(this::handlerPUsr);
		router.put("/put/disp")
				.handler(this::handlerPDisp);
		router.put("/put/relacionUD")
				.handler(this::handlerUD);
		router.put("/put/estado")
				.handler(this::handlerPEstado);
		router.put("/put/historico")
				.handler(this::handlerPHistorico);
		
		/*
		 *	Informacion de las URL para las peticiones PUT:
		 	- /put/usr : Nos permite añadir un usuario pasandole como JSON los siguientes campos: {aliasUsuario, correo, contrasena}.
		 	- /put/disp : Nos permite añadir un nuevo dispositivo pasandole como JSON el campo : {aliasDisp}.
		 	- /put/relacionUD: Nos permite establecer la relacion usuario-dispositivo, pasandole como JSON: {aliasusuario, aliasDisp}
		 	- /put/estado : Nos permite añadir un estado nuevo, pasandole como JSON los campos: {aliasDispositivo, aliasEnchufe, estado_enchufe}.
		 	- /put/historico : Nos permite añadir un nuevo historico al enchufe "aliasEnchufe", 
		 			del dispositivo "aliasDispositivo", con el consumo "consumo". Pasandole como parametros: {aliasDispositivo, aliasEnchufe, consumo}
		 */
		
	}
	
	
	//Funciones para las peticiones GET:
	 
	/**
	 * Funcion para la peticion GET: /usr/:usr/info
	 * @return La informacion del usuario usr
	 * @param routingContext
	 */
	private void handlerUsr(RoutingContext routingContext) {
		//Consigo el parametro que me interesa de la url
		String paramStr = routingContext.pathParam("usr");
		
		//Creo la conexion con la base de datos
		mySQLClient.getConnection(connection ->{
			if(connection.succeeded()) {
				//Realizo la consulta a la base de datos
				connection.result().query(
						"SELECT * FROM usuario WHERE aliasUsuario = '" + paramStr +"'", result ->{ 
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
	
	/**
	 * Funcion para la peticion GET: /usr/:usr/dispositivo/:dispo/info
	 * @return La informacion del dispositivo dispo si está asociado al usuario usr
	 * @param routingContext
	 */
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
						"	FROM dispositivo INNER JOIN relacionuserdisp ON dispositivo.idDispositivo = relacionuserdisp.idDispositivo" + 
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
	
	/**
	 * Funcion para la peticion GET: /usr/:usr/dispositivo/all
	 * @return La informacion de todos los dispositivos asociados al usuario usr
	 * @param routingContext
	 */
	private void handlerAllDisp(RoutingContext routingContext) {
		//Obtengo los parametros de la url que necesito
		String paramUs = routingContext.pathParam("usr");
		//Creo una conexion con la ddbb
		mySQLClient.getConnection(connection ->{
			if(connection.succeeded()) {
				connection.result().query(
						//Realizo la peticion a la base de datos
						"SELECT dispositivo.idDispositivo, aliasDisp" + 
						"	FROM dispositivo INNER JOIN relacionuserdisp ON dispositivo.idDispositivo = relacionuserdisp.idDispositivo" + 
						"				     INNER JOIN usuario ON relacionuserdisp.idUsuario = usuario.idUsuario where usuario.aliasUsuario = '"+ paramUs +"';", result ->{		//result -> resultado de la conexion
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
	
	/**
	 * Funcion para la peticion GET: /usr/:usr/dispositivo/:dispo/enchufe/all
	 * @return La informacion de todos los enchufes del dispositivo dispo asociados al usuario usr
	 * @param routingContext
	 */
	private void handlerAllEnch(RoutingContext routingContext) {
		//Obtengo los parametros de la url que necesito
		String paramUs = routingContext.pathParam("usr");
		String paramDis = routingContext.pathParam("dispo");
		
		//Creo una conexion con la ddbb
		mySQLClient.getConnection(connection ->{
			if(connection.succeeded()) {
				connection.result().query(
						//Realizo la peticion a la base de datos
						"SELECT enchufe.idEnchufe, enchufe.aliasEnchufe" + 
						"		FROM enchufe where enchufe.idDispositivo = (SELECT dispositivo.idDispositivo" + 
						"			FROM dispositivo INNER JOIN relacionuserdisp ON dispositivo.idDispositivo = relacionuserdisp.idDispositivo" + 
						"							 INNER JOIN usuario ON relacionuserdisp.idUsuario = usuario.idUsuario where usuario.aliasUsuario = '"+ paramUs +"' and aliasDisp = '"+ paramDis +"');", result ->{		//result -> resultado de la conexion
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
	
	/**
	 * Funcion para la peticion GET: /usr/:usr/dispositivo/:dispo/enchufe/:ench/info
	 * @return La informacion del enchufe ench del dispositivo dispo asociados al usuario usr
	 * @param routingContext
	 */
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
	
	/**
	 * Funcion para la peticion GET: /usr/:usr/dispositivo/:dispo/enchufe/:ench/estado
	 * @return El estado del enchufe ench del dispositivo dispo asociados al usuario usr
	 * @param routingContext
	 */
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
	
	/**
	 * Funcion para la peticion GET: /usr/:usr/dispositivo/:dispo/enchufe/:ench/historico
	 * @return El historico del enchufe ench del dispositivo dispo asociados al usuario usr
	 * @param routingContext
	 */
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
	
	/**
	 *	Funcion para la peticion PUT: /put/usr
	 	Para ello, en POSTMAN, realizar la petición: 
	 		localhost:8090/put/usr
	 		Body:
	 			{
					"aliasUsuario" : "nombre",
					"correo" : "correo_nombre",
					"contrasena" : "contra_nombre"
				}
	 * @param routingContext
	 */
	private void handlerPUsr(RoutingContext routingContext) {
		//Objeto que obtenemos de la petición PUT
		JsonObject json = routingContext.getBodyAsJson();
		
		//Extraemos lo que necesitamos:
		String paramUsuario = json.getString("aliasUsuario");
		String paramCorreo = json.getString("correo");
		String paramContrasena = json.getString("contrasena");
		
		mySQLClient.getConnection(connection ->{
			if(connection.succeeded()) {
				//Realizo la consulta a la base de datos.
				connection.result().query(
						//Realizo la insercion en la base de datos.
						"INSERT INTO usuario ( idUsuario, aliasUsuario, correo, contrasena )" + 
						"   VALUES" + 
						"   	(null , '"+paramUsuario+"', '"+paramCorreo+"', '"+paramContrasena+"');", result ->{						
						
							if(result.succeeded()) {
								//Si la peticion es correcta, le devuelvo al cliente los datos que nos ha pasado.
								routingContext.response()
									.setStatusCode(201)
										.putHeader("content-type", "application/json; charset=utf-8")
											.end(json.encode());
								
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

	/**
	 *	Funcion para la peticion PUT: /put/disp
	 	Para ello, en POSTMAN, realizar la petición: 
	 		localhost:8090/put/disp
	 		Body:
	 			{
					"aliasDisp" : "nombre"
				}
	 * @param routingContext
	 */
	private void handlerPDisp(RoutingContext routingContext) {
		//Objeto que obtenemos de la petición PUT
		JsonObject json = routingContext.getBodyAsJson();
		
		//Extraemos lo que necesitamos:
		String paramAlias = json.getString("aliasDisp");
		
		mySQLClient.getConnection(connection ->{
			if(connection.succeeded()) {
				//Realizo la consulta a la base de datos.
				connection.result().query(
						//Realizo la insercion en la base de datos.
						"INSERT INTO dispositivo (idDispositivo, aliasDisp)" + 
						"   VALUES" + 
						"   	(null , '"+paramAlias+"');", result ->{						
						
							if(result.succeeded()) {
								//Si la peticion es correcta, le devuelvo al cliente los datos que nos ha pasado.
								routingContext.response()
									.setStatusCode(201)
										.putHeader("content-type", "application/json; charset=utf-8")
											.end(json.encode());
								
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
	
	/**
	 *	Funcion para la peticion PUT: /put/estado
	 	Para ello, en POSTMAN, realizar la petición: 
	 		localhost:8090/put/estado
	 		Body:
	 			{
	 				"aliasDispositivo : "nombre",
					"aliasEnchufe" : "nombre",
					"estado_enchufe" : 0 ó 1
				}
				
		**Los parametros idEstado, idEnchufe y fecha lo precalculamos
		*	a la hora de hacer la inserción de los datos.
		*TODO: Fecha no lo hace correctamente.
	 * @param routingContext
	 */
	private void handlerPEstado(RoutingContext routingContext) {
		//Objeto que obtenemos de la petición PUT
		JsonObject json = routingContext.getBodyAsJson();
		
		//Extraemos lo que necesitamos:
		String paramAliasDisp = json.getString("aliasDispositivo");
		String paramAliasEnch = json.getString("aliasEnchufe");
		Integer paramEstado = json.getInteger("estado_enchufe");
		
		mySQLClient.getConnection(connection ->{
			if(connection.succeeded()) {
				//Realizo la consulta a la base de datos.
				connection.result().query(
						//Realizo la insercion en la base de datos.
						"INSERT INTO estado (idEstado, idEnchufe, estado_enchufe, fecha) " + 
						"VALUES" + 
						"	(null , " + 
						"		(select idEnchufe from enchufe where enchufe.aliasEnchufe = '"+ paramAliasEnch + "' " + 
						"			and enchufe.idDispositivo = (select idDispositivo from dispositivo where aliasDisp = '" + paramAliasDisp + "' ))," + 
						"				" + paramEstado + ", "+ System.currentTimeMillis()/1000 +");	", result ->{
						 
							if(result.succeeded()) {
								//Si la peticion es correcta, le devuelvo al cliente los datos que nos ha pasado.
								routingContext.response()
									.setStatusCode(201)
										.putHeader("content-type", "application/json; charset=utf-8")
											.end(json.encode());
								
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
	

	/**
	 *	Funcion para la peticion PUT: /put/historico
	 	Para ello, en POSTMAN, realizar la petición: 
	 		localhost:8090/put/historico
	 		Body:
	 			{
	 				"aliasDispositivo : "nombre",
					"aliasEnchufe" : "nombre",
					"consumo" : double
					
				}
				
		**Los parametros idEstado, idEnchufe y fecha lo precalculamos
		*	a la hora de hacer la inserción de los datos.
	 * @param routingContext
	 */
	private void handlerPHistorico(RoutingContext routingContext) {
		//Objeto que obtenemos de la petición PUT
		JsonObject json = routingContext.getBodyAsJson();
		
		//Extraemos lo que necesitamos:
		String paramAliasDisp = json.getString("aliasDispositivo");
		String paramAliasEnch = json.getString("aliasEnchufe");
		Integer paramConsumo = json.getInteger("consumo");
		
		mySQLClient.getConnection(connection ->{
			if(connection.succeeded()) {
				//Realizo la consulta a la base de datos.
				connection.result().query(
						//Realizo la insercion en la base de datos.
						"INSERT INTO historico (idhistorico, idEnchufe, consumo, fecha)" + 
						"	VALUES " + 
						"		(null, " + 
						"			(select idEnchufe from enchufe where enchufe.aliasEnchufe = '" + paramAliasEnch +"' " + 
						"					and enchufe.idDispositivo = (select idDispositivo from dispositivo where aliasDisp = '" + paramAliasDisp+ "' ))" + 
						"				," + paramConsumo + " " + 
						"					, "+ System.currentTimeMillis()/1000 +");", result ->{
						
							if(result.succeeded()) {
								//Si la peticion es correcta, le devuelvo al cliente los datos que nos ha pasado.
								routingContext.response()
									.setStatusCode(201)
										.putHeader("content-type", "application/json; charset=utf-8")
											.end(json.encode());
								
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
	
	/**
	 *	Funcion para la peticion PUT: /put/relacionUD
	 	Para ello, en POSTMAN, realizar la petición: 
	 		localhost:8090/put/relacionUD
	 		Body:
	 			{
	 				"aliasUsuario : "nombre",
					"aliasDispositivo" : "nombre"
				}
				
	 * @param routingContext
	 */
	private void handlerUD(RoutingContext routingContext) {
		//Objeto que obtenemos de la petición PUT
		JsonObject json = routingContext.getBodyAsJson();
		
		//Extraemos lo que necesitamos:
		String paramAliasUsuario = json.getString("aliasUsuario");
		String paramAliasDisp = json.getString("aliasDispositivo");
		
		mySQLClient.getConnection(connection ->{
			if(connection.succeeded()) {
				//Realizo la consulta a la base de datos.
				connection.result().query(
						//Realizo la insercion en la base de datos.
						"insert into relacionuserdisp (idDispositivo, idUsuario, idRelacion)" + 
						" values " + 
						"		((select idDispositivo from dispositivo where aliasDisp = '" + paramAliasDisp + "')," + 
						"			(select idUsuario from usuario where aliasUsuario = '" + paramAliasUsuario+ "')," + 
						"				null);", result ->{
						
							if(result.succeeded()) {
								//Si la peticion es correcta, le devuelvo al cliente los datos que nos ha pasado.
								routingContext.response()
									.setStatusCode(201)
										.putHeader("content-type", "application/json; charset=utf-8")
											.end(json.encode());
								
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
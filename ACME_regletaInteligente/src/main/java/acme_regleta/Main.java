package acme_regleta;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
//import io.vertx.core.http.HttpServer;

public class Main extends AbstractVerticle { // Es un verticle

	public void start(Future<Void> startFuture) {	//Despliega la funcionalidad de verticle
		
		//API RES modificado - Proyecto
		vertx.deployVerticle(new ResServerRegleta());
		
		//SERVIDOR TCP PARA USO DE API REST:->
		
		//MQTT
		vertx.deployVerticle(new Mqtt());
		
	}


}
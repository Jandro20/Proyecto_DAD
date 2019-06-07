#include <Arduino.h>
#include <ESP8266WiFi.h>
#include <PubSubClient.h>
#include <ESP8266HTTPClient.h>
#include <ArduinoJson.h>

const char* ssid = "Regleta";
const char* password = "r1234567";
const char* channel_name = "main_topic";  //TOPIC
//IP del ordenador a la hora de ejecutarlo.
const char* mqtt_server = "192.168.43.158";
const char* http_server = "192.168.43.158";
const char* http_server_port = "8090";
String clientId;

WiFiClient espClient;
PubSubClient client(espClient);
long lastMsg = 0;
long lastMsgRest = 0;
char msg[50];
int value = 0;
int msgFail = 0;
int msgRight = 0;

//Definicion de pineS

const int pines = 4;
int array [pines] = {D1,D2,D3,D4};

// Conexión a la red WiFi
void setup_wifi() {

  delay(10);

  // Fijamos la semilla para la generación de número aleatorios. Nos hará falta
  // más adelante para generar ids de clientes aleatorios
  randomSeed(micros());

  Serial.println();
  Serial.print("Conectando a la red WiFi ");
  Serial.println(ssid);

  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);

  // Mientras que no estemos conectados a la red, seguimos leyendo el estado
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  // En este punto el ESP se encontrará registro en la red WiFi indicada, por
  // lo que es posible obtener su dirección IP
  Serial.println("");
  Serial.println("WiFi conectado");
  Serial.println("Dirección IP registrada: ");
  Serial.println(WiFi.localIP());
}

// Método llamado por el cliente MQTT cuando se recibe un mensaje en un canal
// al que se encuentra suscrito. Los parámetros indican el canal (topic),
// el contenido del mensaje (payload) y su tamaño en bytes (length).
//Procesamiento del mensaje para saber que quiere que hagamos y hacerlo.
void callback(char* topic, byte* payload, unsigned int length) {
  Serial.print("Mensaje recibido [canal: ");
  Serial.print(topic);
  Serial.print("] ");
  // Leemos la información del cuerpo del mensaje. Para ello no sólo necesitamos
  // el puntero al mensaje, si no su tamaño.
  for (unsigned int i = 0; i < length; i++) {
    Serial.print((char)payload[i]);
  }
  Serial.println();
  DynamicJsonDocument doc(length);
  deserializeJson(doc, payload, length);
  const char* action = doc["action"];
  const int position = doc["position"];

  Serial.printf("action: %s\n", action);
  Serial.printf("position: %i\n", position);

  } else if(strcmp(action, "encender") == 0){

    digitalWrite(array[position-1], HIGH);
    Serial.print("Procedimiento de encendido para el enchufe ");
    Serial.println(array[position]);

  }else if(strcmp(action, "apagar") == 0){

    digitalWrite(array[position-1], LOW);
    Serial.print("Procedimiento de apagado para el enchufe ");
    Serial.println(array[position-1]);

  }else{
    Serial.println("Accion no reconocida");
  }
}

// Función para la reconexión con el servidor MQTT y la suscripción al canal
// necesario. También se fija el identificador del cliente
void reconnect() {
  // Esperamos a que el cliente se conecte al servidor
  while (!client.connected()) {
    Serial.print("Conectando al servidor MQTT...");
    // Creamos un identificador de cliente aleatorio. Cuidado, esto debe
    // estar previamente definido en un entorno real, ya que debemos
    // identificar al cliente de manera unívoca en la mayoría de las ocasiones
    clientId = "ESP8266Client-";
    clientId += String(random(0xffff), HEX);
    // Intentamos la conexión del cliente

    if (client.connect(clientId.c_str())) {
      String printLine = "   Cliente " + clientId + " conectado al servidor" + mqtt_server;
      Serial.println(printLine);
      // Publicamos un mensaje en el canal indicando que el cliente se ha
      // conectado. Esto avisaría al resto de clientes que hay un nuevo
      // dispositivo conectado al canal. Puede ser interesante en algunos casos.
      String body = "Dispositivo con ID = ";
      body += clientId;
      body += " conectado al canal ";
      body += channel_name;
      //publish -> Publicar algo en el canal
      client.publish(channel_name, body.c_str());
      // Y, por último, suscribimos el cliente al canal para que pueda
      // recibir los mensajes publicados por otros dispositivos suscritos.
      client.subscribe(channel_name);
    } else {
      Serial.print("Error al conectar al canal, rc=");
      Serial.print(client.state());
      Serial.println(". Intentando de nuevo en 5 segundos.");
      delay(5000);
    }
  }
}

// Método para hacer una petición GET al servidor REST
void makeGetRequest(){
    HTTPClient http;
    // Abrimos la conexión con el servidor REST y definimos la URL del recurso
    String url = "http://";
    url += http_server;
    url += ":";
    url += http_server_port;
    url += "/";
    url += "usr";
    url += "/";
    url += "jose";
    url += "/";
    url += "info";
    String message = "Enviando petición GET al servidor REST. ";
    message += url;
    Serial.println(message);
    //Gestion de la peticion
    //Preparamos la llamada (aun no lo ha hecho)
    http.begin(url);
    // Realizamos la petición y obtenemos el código de estado de la respuesta
      //Si fuese un PUT -> http.PUT(); [BLOQUEANTE < 100 ms]
    int httpCode = http.GET();

    if (httpCode > 0)
    {
      msgRight++;
     // Si el código devuelto es > 0, significa que tenemos respuesta, aunque
     // no necesariamente va a ser positivo (podría ser un código 400).
     // Obtenemos el cuerpo de la respuesta y lo imprimimos por el puerto serie
     String payload = http.getString();
     Serial.println("payload: " + payload);

     //A partir de aqui, customizable para nuestro proyecto.
     const size_t bufferSize = JSON_OBJECT_SIZE(1) + 500;
     DynamicJsonDocument root(bufferSize);
     deserializeJson(root, payload);

     //Propiedades del JSON.
     const char* idUsuario = root["idUsuario"];
     const char* aliasUsuario = root["aliasUsuario"];
     const char* correo = root["correo"];
     const char* contrasena = root["contrasena"];

     Serial.print("idUsuario:");
     Serial.println(idUsuario);
     Serial.print("aliasUsuario:");
     Serial.println(aliasUsuario);
     Serial.print("correo:");
     Serial.println(correo);
     Serial.print("contrasena:");
     Serial.println(contrasena);

   }else{
     msgFail++;
   }

    Serial.printf("\nRespuesta servidor REST %d\n", httpCode);
    // Cerramos la conexión con el servidor REST
    http.end();
}

// Método para hacer una petición PUT al servidor REST
void makePutRequest(int potencia){
    HTTPClient http;
    // Abrimos la conexión con el servidor REST y definimos la URL del recurso
    String url = "http://";
    url += http_server;
    url += ":";
    url += http_server_port;
    url += "/put";
    url += "/historico";

    String message = "Enviando petición PUT al servidor REST. ";
    message += url;
    Serial.println(message);
    // Realizamos la petición y obtenemos el código de estado de la respuesta
    http.begin(url);

    const size_t bufferSize = JSON_OBJECT_SIZE(1) + 370;
    DynamicJsonDocument root(bufferSize);

    root["aliasDispositivo"] = "disp_01";
    root["aliasEnchufe"] = "ench_01";
    root["consumo"] = potencia;

    String json_ser;

    serializeJson(root, json_ser);

    int httpCode = http.PUT(json_ser);

    if (httpCode > 0)
    {
     // Si el código devuelto es > 0, significa que tenemos respuesta, aunque
     // no necesariamente va a ser positivo (podría ser un código 400).
     // Obtenemos el cuerpo de la respuesta y lo imprimimos por el puerto serie
     // String payload = http.getString();
     Serial.println("payload put: " + http.getString());
    }

    Serial.printf("\nRespuesta servidor REST PUT %d\n", httpCode);
    // Cerramos la conexión con el servidor REST. [IMPORTANTE, para que no se quede esperando]
    http.end();
}

// Método de inicialización de la lógica
void setup() {
  // Ajustamos el pinmode del pin de salida para poder controlar un led
  pinMode(D1,OUTPUT);
  pinMode(D2,OUTPUT);
  pinMode(D3,OUTPUT);
  pinMode(D4,OUTPUT);

  pinMode(A0, INPUT);

  // Fijamos el baudrate del puerto de comunicación serie
  Serial.begin(115200);
  // Nos conectamos a la red WiFi
  setup_wifi();
  // Indicamos la dirección y el puerto del servidor donde se encuentra el
  // servidor MQTT
  client.setServer(mqtt_server, 1883);
  // Fijamos la función de callback que se ejecutará cada vez que se publique
  // un mensaje por parte de otro dispositivo en un canal al que el cliente
  // actual se encuentre suscrito.
  //Gestiona la recepcion de cada mensaje.
  client.setCallback(callback);
}

float intensidad = 0.0;
int potencia = 0;

//Sensor acs712
float getCorriente(int intentos)
{
   float voltaje;
   float sumaCorriente = 0;
   for (int i = 0; i < intentos; i++)
   {
      voltaje = analogRead(A0) * 5.0 / 1023.0;
      sumaCorriente += (voltaje - 2.5) / 0.066; //Sensibilidad para 30A = 0.066 mA.
   }
   return(sumaCorriente / intentos);
}

void loop() {

  // Nos conectamos al servidor MQTT en caso de no estar conectado previamente
  //Aunque en reconnect ya lo comprueba, asi optimizamos un poco mas.
  if (!client.connected()) {
    reconnect();
  }
  // Esperamos (de manera figurada) a que algún cliente suscrito al canal
  // publique un mensaje que será recibido por el dispositivo actual
  //Gestiona las esperas. Se pregunta si hay algun mensaje
  client.loop();

  long now = millis();

  if(now - lastMsgRest > 60000){
    //Sensor ac712
    intensidad = getCorriente(1);
    potencia = 220.0 * 0.707 * intensidad;
    makePutRequest(potencia);
  }

  if(now - lastMsgRest > 120000){
    makeGetRequest();
  }
}

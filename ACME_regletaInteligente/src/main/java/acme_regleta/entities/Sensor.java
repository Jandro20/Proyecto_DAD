package acme_regleta.entities;

//se tiene que llamar de la misma forma que la de la base de datos.
//Una clase por cada tabla.

/*
 * Importante:
 * 	Crear constructor
 * 	Constructor por defecto
 * 	Atributos setter y getter de todos los atributos
 * 	Tienen que llamarse igual que en la base de datos
 */

public class Sensor {
	private int idsensor;
	private int idUsuario;
	private String value;
	
	
	public Sensor(int idsensor, int idUsuario, String value) {
		super();
		this.idsensor = idsensor;
		this.idUsuario = idUsuario;
		this.value = value;
	}
	
	//Constructor por defecto. Llamando al de arriba con los valores por defecto.
	public Sensor() {
		this(0,0,"");
	}

	public int getIdsensor() {
		return idsensor;
	}

	public void setIdsensor(int idsensor) {
		this.idsensor = idsensor;
	}

	public int getIdUsuario() {
		return idUsuario;
	}

	public void setIdUsuario(int idUsuario) {
		this.idUsuario = idUsuario;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
	
	
	
}

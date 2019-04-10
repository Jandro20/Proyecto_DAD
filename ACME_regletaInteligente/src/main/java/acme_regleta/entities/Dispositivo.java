package acme_regleta.entities;

public class Dispositivo {

	private int idDispositivo;
	private String aliasDisp;
	
	
	public Dispositivo(int idDispositivo, String aliasDisp) {
		super();
		this.idDispositivo = idDispositivo;
		this.aliasDisp = aliasDisp;
	}
	
	public Dispositivo() {
		this(0, " ");
	}

	public int getIdDispositivo() {
		return idDispositivo;
	}

	public void setIdDispositivo(int idDispositivo) {
		this.idDispositivo = idDispositivo;
	}

	public String getAliasDisp() {
		return aliasDisp;
	}

	public void setAliasDisp(String aliasDisp) {
		this.aliasDisp = aliasDisp;
	}
	
	
	
}

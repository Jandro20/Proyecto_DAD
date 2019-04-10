package acme_regleta.entities;

public class Historico {

	private int idhistorico;
	private int idEnchufe;
	private int consumo;
	private Float fecha;
	
	
	public Historico(int idhistorico, int idEnchufe, int consumo, Float fecha) {
		super();
		this.idhistorico = idhistorico;
		this.idEnchufe = idEnchufe;
		this.consumo = consumo;
		this.fecha = fecha;
	}
	
	public Historico() {
		this(0, 0, 0, 0f);
	}

	public int getidhistorico() {
		return idhistorico;
	}

	public void setidhistorico(int idhistorico) {
		this.idhistorico = idhistorico;
	}

	public int getIdEnchufe() {
		return idEnchufe;
	}

	public void setIdEnchufe(int idEnchufe) {
		this.idEnchufe = idEnchufe;
	}

	public int getConsumo() {
		return consumo;
	}

	public void setConsumo(int consumo) {
		this.consumo = consumo;
	}

	public Float getFecha() {
		return fecha;
	}

	public void setFecha(Float fecha) {
		this.fecha = fecha;
	}
	
	
	
}

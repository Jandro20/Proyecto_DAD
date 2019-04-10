package acme_regleta.entities;

public class Estado {

	private int idEstado;
	private int idEnchufe;
	private int estado_enchufe;
	private Integer fecha;
	
	
	public Estado(int idEstado, int idEnchufe, int estado_enchufe, Integer fecha) {
		super();
		this.idEstado = idEstado;
		this.idEnchufe = idEnchufe;
		this.estado_enchufe = estado_enchufe;
		this.fecha = fecha;
	}
	
	
	public Estado() {
		this(0, 0, 0, 0);
	}


	public int getIdEstado() {
		return idEstado;
	}


	public void setIdEstado(int idEstado) {
		this.idEstado = idEstado;
	}


	public int getIdEnchufe() {
		return idEnchufe;
	}


	public void setIdEnchufe(int idEnchufe) {
		this.idEnchufe = idEnchufe;
	}


	public int getEstado_enchufe() {
		return estado_enchufe;
	}


	public void setEstado_enchufe(int estado_enchufe) {
		this.estado_enchufe = estado_enchufe;
	}


	public Integer getFecha() {
		return fecha;
	}


	public void setFecha(Integer fecha) {
		this.fecha = fecha;
	}
	
	
	
}

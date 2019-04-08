package acme_regleta.entities;

public class Usuario {

	private int idUsuario;
	private String aliasUsuario;
	private String correo;
	private String contrasena;
	
	
	public Usuario(int idUsuario, String aliasUsuario, String correo, String contrasena) {
		super();
		this.idUsuario = idUsuario;
		this.aliasUsuario = aliasUsuario;
		this.correo = correo;
		this.contrasena = contrasena;
	}
	
	public Usuario() {
		this(0," "," "," ");
	}

	public int getIdUsuario() {
		return idUsuario;
	}

	public void setIdUsuario(int idUsuario) {
		this.idUsuario = idUsuario;
	}

	public String getAliasUsuario() {
		return aliasUsuario;
	}

	public void setAliasUsuario(String aliasUsuario) {
		this.aliasUsuario = aliasUsuario;
	}

	public String getCorreo() {
		return correo;
	}

	public void setCorreo(String correo) {
		this.correo = correo;
	}

	public String getContrasena() {
		return contrasena;
	}

	public void setContrasena(String contrasena) {
		this.contrasena = contrasena;
	}
	
	
	
}

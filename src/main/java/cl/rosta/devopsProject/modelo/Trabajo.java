package cl.rosta.devopsProject.modelo;

import org.springframework.data.annotation.Id;

public class Trabajo {

	@Id
    private Long id_trabajo;
    private String nombre_trabajo;
    
	public Long getId_trabajo() {
		return id_trabajo;
	}
	public void setId_trabajo(Long id_trabajo) {
		this.id_trabajo = id_trabajo;
	}
	public String getNombre_trabajo() {
		return nombre_trabajo;
	}
	public void setNombre_trabajo(String nombre_trabajo) {
		this.nombre_trabajo = nombre_trabajo;
	}
	
	@Override
	public String toString() {
		return "Trabajo [id_trabajo=" + id_trabajo + ", nombre_trabajo=" + nombre_trabajo + "]";
	}

	
}

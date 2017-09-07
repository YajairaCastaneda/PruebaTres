package wcorp.bprocess.renta.to;

import java.io.Serializable;
import java.util.Date;

/**
 * Objeto que permite almacenar el la información de un cliente que ha recibido el ofrecimiento de actualizar
 * su renta.
 * 
 * <b>Registro de versiones:</b>
 * <ul>
 * 
 * <li>1.0 01/08/2016, Pedro Carmona Escobar (SEnTRA) - Sergio Bravo (Ing. Soft. BCI): versión inicial. </li>
 * </ul>
 * <p>
 * <B>Todos los derechos reservados por Banco de Crédito e Inversiones.</B>
 * <P>
 */ 

public class RegistroActualizaRentaTO implements Serializable {

   
    /**
     * Serial de la clase.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Atributo que contiene el rut del cliente.
     */
    private long rut;
    
    /**
     * Atributo que contiene el dígito verificador del rut.
     */
    private char dv;

    /**
     * Atributo que contiene el ID de ATM.
     */
    private String idATM;

    /**
     * Atributo que contiene la fecha del registro.
     */
    private Date fechaRegistro;
    
    /**
     * Atributo que contiene el ID del canal por el cual se hace el registro.
     */
    private String idCanal;
    
    /**
     * Atributo que contiene el ID numérico de la campaña asociada al registro.
     */
    private int idCampana;
    
    /**
     * Atributo que contiene cantidad de iteraciones de actualización de renta que se han realizado.
     */
    private int iteracion;
    
    /**
     * Atributo que contiene el carácter 'S' o 'N' que representa si el cliente aceptó o no actualizar la renta.
     */
    private char respuestaCliente;
    
    
    /**
     * Atributo que contiene el código de error del proceso de actualizar la renta.
     */
    private String codError;
    
    /**
     * Atributo que contiene la glosa asociada al código de error..
     */
    private String glosaError;


    public long getRut() {
        return rut;
    }

    public void setRut(long rut) {
        this.rut = rut;
    }

    public char getDv() {
        return dv;
    }

    public void setDv(char dv) {
        this.dv = dv;
    }

    public String getIdATM() {
        return idATM;
    }

    public void setIdATM(String idATM) {
        this.idATM = idATM;
    }

    public Date getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(Date fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getIdCanal() {
        return idCanal;
    }

    public void setIdCanal(String idCanal) {
        this.idCanal = idCanal;
    }

    public int getIdCampana() {
        return idCampana;
    }

    public void setIdCampana(int idCampana) {
        this.idCampana = idCampana;
    }

    public int getIteracion() {
        return iteracion;
    }

    public void setIteracion(int iteracion) {
        this.iteracion = iteracion;
    }

    public char getRespuestaCliente() {
        return respuestaCliente;
    }

    public void setRespuestaCliente(char respuestaCliente) {
        this.respuestaCliente = respuestaCliente;
    }

    public String getCodError() {
        return codError;
    }

    public void setCodError(String codError) {
        this.codError = codError;
    }

    public String getGlosaError() {
        return glosaError;
    }

    public void setGlosaError(String glosaError) {
        this.glosaError = glosaError;
    }

    
    public String toString() {
        StringBuffer builder = new StringBuffer();
        builder.append("RegistroActualizaRentaTO [rut=");
        builder.append(rut);
        builder.append(", dv=");
        builder.append(dv);
        builder.append(", idATM=");
        builder.append(idATM);
        builder.append(", fechaRegistro=");
        builder.append(fechaRegistro);
        builder.append(", idCanal=");
        builder.append(idCanal);
        builder.append(", idCampana=");
        builder.append(idCampana);
        builder.append(", iteracion=");
        builder.append(iteracion);
        builder.append(", respuestaCliente=");
        builder.append(respuestaCliente);
        builder.append(", codError=");
        builder.append(codError);
        builder.append(", glosaError=");
        builder.append(glosaError);
        builder.append("]");
        return builder.toString();
    }


    
}

package wcorp.bprocess.renta;

import java.rmi.RemoteException;
import java.util.Date;
import java.util.Map;

import javax.ejb.CreateException;
import javax.ejb.EJBObject;
import javax.naming.NamingException;

import wcorp.aplicaciones.productos.colocaciones.solicitudes.instantaneas.to.DatosBiometricosTO;
import wcorp.aplicaciones.productos.colocaciones.solicitudes.instantaneas.to.RentaSolicitanteTO;
import wcorp.bprocess.renta.to.EncabezadoCalculoAutomaticoTO;
import wcorp.bprocess.renta.to.RegistroActualizaRentaTO;
import wcorp.bprocess.renta.to.ResultadoRegistroRentaTO;
import wcorp.bprocess.renta.vo.CotizacionClienteVO;
import wcorp.bprocess.renta.vo.DetalleCotizacionClienteVO;
import wcorp.bprocess.renta.vo.ErrorCotizacionVO;
import wcorp.serv.clientes.ClientesException;
import wcorp.serv.renta.CalculoHistorico;
import wcorp.serv.renta.CalculoRentaException;
import wcorp.serv.renta.Dai;
import wcorp.serv.renta.LiquidacionDeSueldo;
import wcorp.serv.renta.RentaCliente;
import wcorp.serv.renta.to.CalculoRentaTO;
import wcorp.serv.renta.to.DetalleArriendoTO;
import wcorp.util.GeneralException;
import wcorp.util.bee.MultiEnvironment;
import wcorp.util.com.TuxedoException;

/**
 * <b>CalculoRenta</b>
 *
 * Interfaz que contiene todos los métodos que tienen relación directa 
 * con el EJB CalculoRentaBean.
 *  
 * <p>Registro de versiones:</p>
 * <ul>
 * <li> 1.0  Sin informacion :  Version Inicial</li>
 * <li> 2.0 11/04/2014 Pedro Rebolledo L. (SEnTRA): Se crean los métodos:
 *                                                  {@link #consultaUltimoCalculo(String, String)}
 *                                                  {@link #eliminarBorradorRenta(CalculoRentaTO)}
 *                                                  {@link #consultarDetalleBienRaiz(long, char, int)}
 *                                                  {@link #ingresarDetalleBienRaiz(long, char, int, DetalleArriendoTO)}
 * <li> 1.1 12/09/2014 Pedro Carmona Escobar (SEnTRA): Se crea el método:
 *                  {@link #obtenerRentaConCotizacionesEquifax(String, long, char, String, String, String, Date).
 * </li>
 * <li> 1.2 19/03/2015 Alejandro Barra (SEnTRA) - Andrés Alvarado (Ing. Soft. BCI): se crean los métodos:
 *            {@link #grabaEncabezadoCalculoAutomatico(EncabezadoCalculoAutomaticoTO)}
 *            {@link #grabaDetallesCalculoAutomatico(DetalleCotizacionClienteVO[], EncabezadoCalculoAutomaticoTO)}
 *            {@link #consultaEncabezadoCalculoAutomatico(int)}
 *            {@link #consultaDetallesCalculoAutomatico(int)}.
 * <li> 1.3 20/03/2016 Andrés Cea S. (TINet) - Oliver Hidalgo (Ing. Soft. BCI): Se agregan los siguientes métodos:
 *        {@link #actualizarRentaClienteCotizaciones(long, char, String, String, int)}
 *        {@link #actualizarRentaProspectoViaje(long, char, String, String, int)}
 * </li>
 * <li> 1.4 02/06/2016 Rafael Pizarro. (TINet) - Oliver Hidalgo (Ing. Soft. BCI): Se modifica el siguiente método:
 *        {@link #actualizarRentaClienteCotizaciones(long, char, String, String}
 *        se genera el nuevo metodo:
 *        {@link #ingresaRentaPreviredDGCRTA(CotizacionClienteVO)}
 *
 * <li> 1.5 21/07/2016 Ariel Acuña (SEnTRA) - Sergio Bravo (Ing. Soft. BCI): se agregan el método:
 *              {@link #ingresarRegistroActualizacionRenta(RegistroActualizaRentaTO)}
 *
 * </li>
 *                      
 * </ul>
 * <B>Todos los derechos reservados por Banco de Crédito e Inversiones.</B>
 * <P>
 *
 * @see CalculoRentaBean
 */

public interface CalculoRenta extends EJBObject {

	/**
     * @see CalculoRentaBean#getEcho()
	 */
	public String getEcho() 
		throws RemoteException;
	
	/**
     * @see CalculoRentaBean#obtieneParametrosCalculo()
	 */
	public CalculoRentaTO obtieneParametrosCalculo(CalculoRentaTO rentaTO) 
		throws CalculoRentaException, RemoteException;

    /**
     * 
     * @see CalculoRentaBean#grabaCalculoRenta(RentaCliente evaluacionCliente)
     */
    public ResultadoRegistroRentaTO grabaCalculoRenta(RentaCliente evaluacionCliente)
        throws TuxedoException, CalculoRentaException,
        ClientesException, GeneralException, RemoteException, NamingException,
        CreateException;
	
	/**
	 * @see CalculoRentaBean#calculoRentaConLiquidacion()
	 */
	public CalculoRentaTO calculoRentaConLiquidacion(CalculoRentaTO rentaTO) 
		throws CalculoRentaException, TuxedoException, RemoteException;
	
	/**
	 * @see CalculoRentaBean#calculoRentaConDai()
	 */
	public CalculoRentaTO calculoRentaConDai(CalculoRentaTO rentaTO) 
		throws CalculoRentaException, RemoteException;
	
	/**
	 * @see CalculoRentaBean#obtieneCalculoHistoricoCliente(boolean esClienteBci, String rutCliente, String dvRut, String ejecutivo)
	 */
	public RentaCliente[] obtieneCalculoHistoricoCliente(boolean esClienteBci, String rutCliente, String dvRut, String ejecutivo, CalculoRentaTO rentaTO)
		throws CalculoRentaException, RemoteException;
		
	/**
	 * @see CalculoRentaBean#obtieneCalculoCliente(boolean esClienteBci, String rutCliente, String dvRut, String ejecutivo, boolean reglaDe15dias)
	 */
	public CalculoRentaTO obtieneCalculoCliente(boolean esClienteBci, String rutCliente, String dvRut, String ejecutivo, boolean reglaDe15dias, CalculoRentaTO rentaTO) 
		throws CalculoRentaException, RemoteException;
	
	/**
	 * @see CalculoRentaBean#setDatosCliente(RentaCliente rCliente)
	 */
	public RentaCliente setDatosCliente(RentaCliente rCliente) 
		throws RemoteException;

	/**
	 * @see CalculoRentaBean#obtieneLiquidacionDeSueldo(String periodo)
	 */
	public  LiquidacionDeSueldo obtieneLiquidacionDeSueldo(String periodo, LiquidacionDeSueldo[] liquidaciones) 
		throws CalculoRentaException, RemoteException;
	
	/**
	 * @see CalculoRentaBean#obtienePrimeraLiquidacionDeSueldo(String ejecutivo)
	 */
	public LiquidacionDeSueldo obtienePrimeraLiquidacionDeSueldo(String ejecutivo, CalculoRentaTO rentaTO) throws CalculoRentaException, RemoteException;
	
	/**
	 * @see CalculoRentaBean#obtienePrimeraLiquidacionLimite(String ejecutivo)
	 */	
	public LiquidacionDeSueldo obtienePrimeraLiquidacionLimite(String ejecutivo, CalculoRentaTO rentaTO) throws CalculoRentaException, RemoteException;
	
	
	/**
	 * @see CalculoRentaBean#obtieneProximaLiquidacionDeSueldo(String periodo, String ejecutivo)
	 */
	public LiquidacionDeSueldo obtieneProximaLiquidacionDeSueldo(String periodo, String ejecutivo, CalculoRentaTO rentaTO) 
		throws CalculoRentaException, RemoteException;
	
	/**
	 * @see CalculoRentaBean#setLiquidacion(LiquidacionDeSueldo liquidacion)
	 */
	public CalculoRentaTO setLiquidacion(LiquidacionDeSueldo liquidacion, CalculoRentaTO rentaTO) 
		throws CalculoRentaException, RemoteException;
	
	/**
	 * @see CalculoRentaBean#obtienePrimeraDai(String ejecutivo)
	 */
	public Dai obtienePrimeraDai(String ejecutivo, CalculoRentaTO rentaTO) 
		throws CalculoRentaException, RemoteException;
	
	/**
	 * @see CalculoRentaBean#obtieneDai(String ejecutivo, String periodo)
	 */
	public Dai obtieneDai(String ejecutivo, String periodo, CalculoRentaTO rentaTO) 
		throws CalculoRentaException, RemoteException;
	
	/**
	 * @see CalculoRentaBean#setDai(Dai dai)
	 */
	public RentaCliente setDai(Dai dai, CalculoRentaTO rentaTO) 
		throws CalculoRentaException, RemoteException;
	
	/**
	 * @see CalculoRentaBean#setBoletaHonorarios(String periodo, float monto)
	 */
	public CalculoRentaTO setBoletaHonorarios(String periodo, float monto, CalculoRentaTO rentaTO) 
		throws CalculoRentaException, RemoteException;

	
	/**
	 * @see CalculoRentaBean#obtieneCalculoHistorico()
	 */
	public CalculoHistorico[] obtieneCalculoHistorico(RentaCliente evaluacionCliente)
		throws CalculoRentaException, RemoteException;
		
	/**
	 * @see CalculoRentaBean#obtieneGlosaOffColaborador(String codOff)
	 */
	public String obtieneGlosaOffColaborador(String codOff) 
	    throws CalculoRentaException, RemoteException;	
	    
	/**
	 * @see CalculoRentaBean#modificaEjecutivo(String ejAct, String ejeIng)
	 */	    
	public boolean modificaEjecutivo(String ejAct, String ejeIng) 
	throws CalculoRentaException, RemoteException;
		
    /**
	 * @see CalculoRentaBean#setRentaFija(float rentaFija,String canal)
     */
    public void setRentaFija(float rentaFija,String canal)
    throws CalculoRentaException, RemoteException;

    /**
     * @see CalculoRentaBean#setIngresosAdicionales(String rut, String dv, int numeroSecuencia,float montoArriendoBienesRaices,float montoPensionJubilacion,float montoPensionAlimenticia,String usuarioIngresa,String periodo)
     */
    public int setIngresosAdicionales(String rut, String dv,
                                              int numeroSecuencia,
                                              float montoArriendoBienesRaices,
                                              float montoPensionJubilacion,
                                              float montoPensionAlimenticia,
                                              String usuarioIngresa,
                                              String periodo)
    throws CalculoRentaException, RemoteException;

    /**
     * @see CalculoRentaBean#obtenerRentaClientePersona(long rut, char dv)
     */
    public double obtenerRentaClientePersona(long rut, char dv) throws Exception,RemoteException;

    /**
     * @see CalculoRentaBean#obtenerValorUF()
     */
    public double obtenerValorUF() throws Exception, RemoteException;

    /**
     * @see CalculoRentaBean#consultaCotizacionCliente(String xmlEntrada)
     */
    public String consultaCotizacionCliente(String xmlEntrada) throws Exception, RemoteException;

	/**
	* @see CalculoRentaBean#ingresoCotizacionCliente(CotizacionClienteVO cotizacionCliente)
	*/
	public int ingresoCotizacionCliente(CotizacionClienteVO cotizacionCliente) throws Exception, RemoteException;

	/**
	* @see CalculoRentaBean#ingresaErrorCotizacion(CotizacionClienteVO cotizacionCliente, ErrorCotizacionVO errorCotizacion)
	*/
	public int ingresaErrorCotizacion(CotizacionClienteVO cotizacionCliente, ErrorCotizacionVO errorCotizacion) throws Exception, RemoteException;        

	/**
	* @see CalculoRentaBean#ingresarEncabezadoCotizacion(CotizacionClienteVO cotizacionCliente)
	*/    	
	public int ingresarEncabezadoCotizacion(CotizacionClienteVO cotizacionCliente) throws Exception, RemoteException;

	/**
	* @see CalculoRentaBean#obtenerConfirmacionConsultaPrevired(long rut, char dv)
	*/    	
	public boolean obtenerConfirmacionConsultaPrevired(long rut, char dv) throws Exception, RemoteException;
    	
	/**
	* @see CalculoRentaBean#obtenerConfirmacionConsultaPreviredFlag(long rut, char dv) 
	*/
	public Map obtenerConfirmacionConsultaPreviredFlag(long rut, char dv) throws Exception, RemoteException;
    	
	/**
	* @see CalculoRentaBean#actualizarRentaConCotizaciones(String canal, long rut, char dv, MultiEnvironment multiEnvironment, String codServicio)
	*/    	
	public boolean actualizarRentaConCotizaciones(String canal, long rut, char dv, MultiEnvironment multiEnvironment, String codServicio) throws Exception, RemoteException;;
    	
	/**
     * @see CalculoRentaBean#obtenerRentaConCotizacionesPrevired(String, long, char, String,
     *      DatosBiometricosTO)
     */
    public RentaSolicitanteTO obtenerRentaConCotizacionesPrevired(String canal, long rut, char dv,
        String idAgenteAtencion, DatosBiometricosTO datosBiometricos) throws GeneralException, RemoteException;
    
    /**
     * @see CalculoRentaBean#actualizaRentasCliente(long, char, RentaCliente)
     */
    public boolean actualizaRentasCliente(long rutcliente, char dvCliente, RentaCliente rentaCliente)
        throws GeneralException, TuxedoException, ClientesException, RemoteException, NamingException,
        CreateException, RemoteException;

    /**
     * <p>Método que permite consultar el detalle de un arriendo de bien raíz, según rut asociado.</p>
     *
     * Registro de versiones:<ul>
     *
     * <li>1.0 15/04/2014 Rodrigo Pino. (SEnTRA): versión inicial.</li>
     *
     * </ul>
     * @param rutCliente RUT del cliente.
     * @param dvRut DV del Rut del cliente.
     * @return RentaCliente con la información consultada. 
     * @throws Exception de la consulta.
     * @throws RemoteException de una exception remota.
     * @since 2.0
     */
    public RentaCliente consultaUltimoCalculo(String rutCliente, String dvRut) throws Exception, RemoteException;
    
    /**
	 * <p>Método que permite consultar el detalle de un arriendo de bien raíz, según rut asociado.</p>
	 *
	 * Registro de versiones:<ul>
	 *
	 * <li>1.0 15/04/2014 Rodrigo Pino. (SEnTRA): versión inicial.</li>
     *
     * </ul>
     *
	 * @param rut rut del arrendatario.
	 * @param dv dv del rut del arrendatario.
	 * @param secuencia del bien raiz.
	 * @return DetalleArriendoTO[] con el detalle del bien raiz.
	 * @throws Exception de la consulta.
	 * @since 2.0
     */
    public DetalleArriendoTO[] consultarDetalleBienRaiz(long rut, char dv, int secuencia)throws Exception;
    
    /**
     * <p>Método que permite ingrsar el detalle de bien raíz, según el rut asociado.</p>
     *
     * Registro de versiones:<ul>
     *
	 * <li>1.0 15/04/2014 Rodrigo Pino. (SEnTRA): versión inicial.</li>
     *
     * </ul>
     *
     * @param rut rut del arrendatario.
     * @param dv dv del rut del arrendatario.
     * @param secuencia del bien raiz.
     * @param detalleArriendoTO TO que contiene el detalle del arriendo.
     * 
     * @return boolean con la respuesta de la inserción.
     * 
     * @throws Exception excepcion de ingreso.
     * @since 2.0
     */
    public boolean ingresarDetalleBienRaiz(long rut, char dv, int secuencia, DetalleArriendoTO detalleArriendoTO) 
    		throws Exception;

    /**
     * Método que elimina un borrador de Renta.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 15/04/2014 Pedro Rebolledo L. (SEnTRA): Versión inicial.
     * </ul>
     * <p>
     * 
     * @param calculoRenta CalculoRentaTO con los datos para la eliminación.
     * @throws RemoteException en caso de error.
     * @throws Exception de la eliminación.
     * @since 2.0
     */
    public void eliminarBorradorRenta(CalculoRentaTO calculoRenta) throws RemoteException, Exception;

    
    /**
     * Método que obtiene las rentas del cliente mediante un cálculo realizado con información de las
     * cotizaciones previsionales obtenidas desde Equifax.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 12/09/2014 Pedro Carmona Escobar (SEnTRA): versión inicial.
     * </ul>
     * <p>
     * 
     * @param canal con el canal por el cual se está realizando el proceso.
     * @param rut identiciador de rut del solicitante.
     * @param dv dígito verificador del rut del solicitante.
     * @param idAgenteAtencion identificador del ejecutivo.
     * @param afp con el nombre de la afp en donde cotiza el rut.
     * @param folioValidacion con el folio de validación.
     * @param fechaCertificado con la fecha de emición del certificado.
     * @return estructura con la información de la renta del cliente de Equifax.
     * @throws RemoteException en caso de ocurrir un error durante la consulta.
     * @throws GeneralException en caso de ocurrir un error durante la consulta.
     * @since 1.1
     */
    public RentaSolicitanteTO obtenerRentaConCotizacionesEquifax(String canal, long rut, char dv,
            String idAgenteAtencion, String afp, String folioValidacion, Date fechaCertificado)
                    throws RemoteException, GeneralException;
    
     /**
       * @see CalculoRentaBean#actualizarRentaConCotizaciones(String canal, long rut, char dv,
       *  MultiEnvironment multiEnvironment, String codServicio, String aplicacion)
       */    	
     public boolean actualizarRentaConCotizaciones(String canal, long rut, char dv, MultiEnvironment
			multiEnvironment, String codServicio,String aplicacion) throws Exception, RemoteException;

    /**
     * @see CalculoRentaBean#grabaEncabezadoCalculoAutomatico(
     *      EncabezadoCalculoAutomaticoTO encabezadoCalculoAutomatico)
     */
    public ResultadoRegistroRentaTO grabaEncabezadoCalculoAutomatico(
           EncabezadoCalculoAutomaticoTO encabezadoCalculoAutomatico) throws RemoteException, Exception;
    
    /**
     * @see CalculoRentaBean#grabaDetallesCalculoAutomatico(DetalleCotizacionClienteVO[] detalleCotizacionCliente,
     *                                                  EncabezadoCalculoAutomaticoTO encabezadoCalculoAutomatico)
     */
    public ResultadoRegistroRentaTO grabaDetallesCalculoAutomatico(
           DetalleCotizacionClienteVO[] detalleCotizacionCliente,
           EncabezadoCalculoAutomaticoTO encabezadoCalculoAutomatico) throws RemoteException, Exception;
    
    /**
     * @see CalculoRentaBean#consultaEncabezadoCalculoAutomatico(int secuencia)
     */    	
    public EncabezadoCalculoAutomaticoTO consultaEncabezadoCalculoAutomatico(int secuencia)
           throws RemoteException, Exception ;

    /**
     * @see CalculoRentaBean#consultaDetallesCalculoAutomatico(int secuencia)
     */
    public DetalleCotizacionClienteVO[] consultaDetallesCalculoAutomatico(int secuencia)
           throws RemoteException, Exception ;
    
    /**
     * @see CalculoRentaBean#actualizarRentaClienteCotizaciones(long rut, char dv, String canal, String
     *      codServicio)
     */
    public String actualizarRentaClienteCotizaciones(long rut, char dv, String canal, String codServicio) 
            throws GeneralException, RemoteException;
    
    /**
     * @see CalculoRentaBean#actualizarRentaProspectoViaje(long rut, char dv, String canal, String codServicio, int
     *      codigoViaje)
     */
    public boolean actualizarRentaProspectoViaje(long rut, char dv, String canal, String codServicio,
        int codigoViaje) throws GeneralException, RemoteException, Exception;
    
    /**
     * @see CalculoRentaBean#ingresaRentaPreviredDGCRTA(CotizacionClienteVO cotizacionCliente)
     */
    public int ingresaRentaPreviredDGCRTA(CotizacionClienteVO cotizacionCliente) throws Exception;

    /**
     * @see CalculoRentaBean#ingresarRegistroActualizacionRenta(RegistroActualizaRentaTO)
     */
    public void ingresarRegistroActualizacionRenta(RegistroActualizaRentaTO registro) throws RemoteException, Exception;

}

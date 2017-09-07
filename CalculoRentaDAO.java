package wcorp.bprocess.renta.dao;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import wcorp.bprocess.renta.reglas.to.CotizacionPreviredTO;
import wcorp.bprocess.renta.reglas.to.CotizacionTO;
import wcorp.bprocess.renta.reglas.to.FiltroConsultaPagoTO;
import wcorp.bprocess.renta.reglas.to.PagoTO;
import wcorp.bprocess.renta.reglas.to.RentaTO;
import wcorp.bprocess.renta.to.EncabezadoCalculoAutomaticoTO;
import wcorp.bprocess.renta.to.RegistroActualizaRentaTO;
import wcorp.bprocess.renta.to.ResultadoRegistroRentaTO;
import wcorp.bprocess.renta.vo.CotizacionClienteVO;
import wcorp.bprocess.renta.vo.DetalleCotizacionClienteVO;
import wcorp.bprocess.renta.vo.ErrorCotizacionVO;
import wcorp.serv.renta.to.CalculoRentaTO;
import wcorp.serv.renta.to.DetalleArriendoTO;
import wcorp.util.DoubleUtl;
import wcorp.util.ErroresUtil;
import wcorp.util.FechasUtil;
import wcorp.util.GeneralException;
import wcorp.util.RUTUtil;
import wcorp.util.StringUtil;
import wcorp.util.TablaValores;

import cl.bci.esb.cliente.antecedentes.renta.ServicioConsultaRentaCliente.v1.ConsultarCotizacionesRequest;
import cl.bci.esb.cliente.antecedentes.renta.ServicioConsultaRentaCliente.v1.ConsultarCotizacionesResponse;
import cl.bci.esb.cliente.antecedentes.renta.ServicioConsultaRentaCliente.v1.DetalleRespuestaCCA;
import cl.bci.esb.cliente.antecedentes.renta.ServicioConsultaRentaCliente.v1.ParametroAUT;
import cl.bci.esb.cliente.antecedentes.renta.ServicioConsultaRentaCliente.v1.ParametroCCA;
import cl.bci.esb.cliente.antecedentes.renta.ServicioConsultaRentaCliente.v1.RespuestaCCA;
import cl.bci.esb.cliente.antecedentes.renta.ServicioConsultaRentaCliente.v1.Rut;
import cl.bci.middleware.aplicacion.persistencia.ConectorServicioDePersistenciaDeDatos;
import cl.bci.middleware.aplicacion.persistencia.ejb.ConfiguracionException;
import cl.bci.middleware.aplicacion.persistencia.ejb.EjecutarException;
import cl.bci.middleware.aplicacion.persistencia.ejb.ServicioDatosException;

import ws.bci.cliente.antecedentes.renta.wscliente.ServicioConsultaRentaCliente;
import ws.bci.cliente.antecedentes.renta.wscliente.ServicioConsultaRentaCliente_Impl;
import ws.bci.cliente.antecedentes.renta.wscliente.ServicioConsultaRentaCliente_PortType;
import ws.bci.cliente.antecedentes.renta.wscliente.ServicioConsultaRentaCliente_PortType_Stub;

/**
 * Data Access Object que accede a la base de datos de bci para consulta de cotizaciones de clientes.
 *
 * <p>Registro de versiones:<ul>
 * <li>1.0   (03/04/2008, Mauricio Retamal C. (SEnTRA)): versión inicial.
 * <li>1.1   (22/04/2009, Pedro Carmona Escobar (SEnTRA)): Se agregan los siguientes métodos: <p>
 *              - {@link #obtieneUltimoRegistroValidoDeCotizaciones(long rut, Date fechaValidez)}<p>
 *           - {@link #obtieneDetalleCotizacion(String folio)}<p>
 *           - {@link #actualizaEstadoEncabezado(int idRegistro, String estado)}<p>
 * <li>2.0 15/04/2014 Rodrigo Pino y Pedro Rebolledo Lagno (SEnTRA):
 *                                     Se modifica la variable {@link #log} según la Normativa Vigente.
 *                                     Se agrega el método {@link #getLogger()}. 
 *                                  Se modifican los import según la Normativa Vigente.
 *                                  Se agregan las variables:
 *                                      {@link #IDENTIFICADOR_ARRIENDO}.
 *                                      {@link #JDBC_DGC}.
 *                                    Se crean los métodos:
 *                                      {@link #eliminarBorradorRenta(CalculoRentaTO)}
 *                                      {@link #consultarDetalleBienRaiz(long, char, int)}
 *                                      {@link #ingresarDetalleBienRaiz(long, char, int, DetalleArriendoTO)}
 * </li>
 * <li>2.1 30/07/2014 Eduardo Villagrán Morales (Imagemaker): se mejora y normaliza log.
 * <li>2.2   (22/09/2014, Manuel Escárate R. (BEE)): Se crea método 
 *            - {@link #ingresarRegistroRTA(CotizacionClienteVO cotizacionCliente))}
 * </li> 
 * <li>2.3 (24/02/2015, Manuel Escárate R. (BEE)): Se agregan nuevos parametros al método 
 *              {@link #ingresarRegistroRTA(CotizacionClienteVO cotizacionCliente))}
 *
 * <li> 3.0 19/03/2015 Alejandro Barra (SEnTRA) - Andrés Alvarado (Ing. Soft. BCI): se agregan los métodos:
 *              {@link #grabaEncabezadoCalculoAutomatico(EncabezadoCalculoAutomaticoTO)}
 *              {@link #grabaDetallesCalculoAutomatico(DetalleCotizacionClienteVO, EncabezadoCalculoAutomaticoTO)}
 *              {@link #consultaEncabezadoCalculoAutomatico(int)}
 *              {@link #consultaDetallesCalculoAutomatico(int)}
 * </li>
 * <li> 3.1 15/03/2016 Andrés Cea S. (TINet), Ignacio González D. (TINet) - Oliver Hidalgo (Ing. Soft. BCI): se agregan los métodos:
 *              {@link #crearClienteWSConsulaPrevired()}
 *              {@link #obtieneCotizacionesWSPrevired(long, char, int)}
 *              {@link #ingresarRentaCalculadaProspecto(RentaTO)}
 *              {@link #ingresoDetalleCotizacionProspecto(RentaTO)}
 *              {@link #actualizarEstadoEncabezadoCotizacionProspecto(int, char)}
 *              {@link #ingresarEncabezadoCotizacionProspecto(RentaTO)}
 *              {@link #consultaPagosBeneficiario(FiltroConsultaPagoTO)}
 *              {@link #retornarValorDelHashMap(Object, String)}
 * </li>
 * <li> 3.2 22/04/2016 Ignacio GonzálezD. (TINet) - Oliver Hidalgo (Ing. Soft. BCI): {@link #consultaPagosBeneficiario(FiltroConsultaPagoTO)}</li>
 * <li> 3.3 24/05/2016 Rafael Pizarro (TINet) - Hernán Rodriguez (TINet) - Claudia López (Ing. Soft. BCI): se modifica el método:
 *              {@link #obtieneCotizacionesWSPrevired(long, char, int)}
 * se crea el método:
 *              {@link #ingresaRentaPreviredDGCRTA(CotizacionClienteVO)}.
 * </li>
 * <li> 3.4 20/07/2016 Hernán Rodriguez (TINet) - Claudia López (Ing. Soft. BCI): asigna ruta tabla de parametros correspondiente a previred.
 *
 * <li> 3.5 21/07/2016 Ariel Acuña (SEnTRA) - Sergio Bravo (Ing. Soft. BCI): se agregan los métodos:
 *              {@link #ingresarRegistroActualizacionRenta(RegistroActualizaRentaTO)}
 * </li>
 * </ul>
 * 
 * <B>Todos los derechos reservados por Banco de Crédito e Inversiones.</B>
 */
 
 public class CalculoRentaDAO {
    /**
     * Identificador de ingreso por arriendo.
     */
    private static final String IDENTIFICADOR_ARRIENDO = "001";
    /**
     * Identificador del JDBC_DGC.
     */
    private static final String JDBC_DGC= "dgc";

	/**
    * Archivo de parámetros cálculo renta.
    */
    private static final String TABLA_PARAMETROS_CALCULORENTA = "CalculoRenta.parametros";
    
    /**
     * Archivo de parámetros cotización Cliente.
     */
     private static final String TABLA_PARAMETROS_COTIZACIONES = "previred/cotizacionCliente.parametros";

	/**
	 * Valor para primer día.
	 */
	private static final String PRIMERDIA = "01";
	
    /**
     * Constante con código de error para caso en que no se pueda actualizar cabecera en bdd de prospectos.
     */
    private static final String ERROR_UPDATE_CABECERA = "ERRACTREN05";
    
    /**
     * Constante con código de error para caso en que no se pueda actualizar estado cabecera en bdd de prospectos.
     */
    private static final String ERROR_UPDATE_ESTADO_CABECERA = "ERRACTREN06";
    
    /**
     * Constante con código de error para caso en que no se pueda ingresar detalle cotizaciones en bdd de prospectos.
     */
    private static final String ERROR_INSERT_COTIZACIONES = "ERRACTREN07";
    
    /**
     * Constante con código de error para caso en que no se pueda consultar pagos del beneficiario.
     */
    private static final String ERROR_CONSULTA_PAGOS = "ERRCALRENTRAN03";
    
    /**
     * Constante con formato de fecha mes-anyo MMyyyy.
     */
    private static final String FORMATO_MMYYYY = "MMyyyy";
    
    /**
     * Constante que define largo de formato MMyyyy.
     */
    private static final int LARGO_FORMATO_MMYYYY = 6;

    /**
     * Variable para obtener los miles.
     */
    private static final int MONTO_MULTI_RENTA = 1000;
    
    /**
     * Variable para utilizar en el período.
     */
    private static final int PRIMER_VALOR_SUB = 2;
    
    /**
     * Variable para utilizar en el período.
     */
    private static final int SEGUNDO_VALOR_SUB = 6;
    
    /**
     * Constante para largo del rut.
     */
    private static final int LARGO_RUT = 8;
    
    /**
     * Valor que representa el codigo de la consulta realizada sin error a Previred.
     */
    private static final String CODIGO_CONSULTA_EXITOSA_PREVIRED = "9050";
    
    /**
     * Variable para asignar log de ejecución.
     */
    private transient Logger logger = (Logger) Logger.getLogger(CalculoRentaDAO.class);
    
    /**
     * Método permite registrar la cotización de un cliente en particular.
     *
	 * <p>Registro de versiones:<UL>
     *  <LI>1.0 03/04/2008 Mauricio Retamal C. (SEnTRA) : versión inicial.
	 * <li>1.1 30/07/2014 Eduardo Villagrán Morales (Imagemaker): se mejora y normaliza log
	 * </UL>
     *
     * @param cotizacionCliente contiene la información necesaria para ingresar una cotización.
	 * @return cantidad registros ingresados.
	 * @throws Exception al haber error. 
     * @since 1.0
     */
	public int ingresoCotizacionCliente(CotizacionClienteVO cotizacionCliente) throws Exception{
	    if (getLogger().isInfoEnabled()) {
            getLogger().info("[ingresoCotizacionCliente] [BCI_INI] cotizacionCliente=<"
                    + cotizacionCliente + ">");
        }
        HashMap parametros = new HashMap();
        Integer registros = null;

        String contexto = TablaValores.getValor("JNDIConfig.parametros", "cluster", "param");
		if (getLogger().isDebugEnabled()) {
            getLogger().debug("[ingresoCotizacionCliente] contexto JNDI=<" + contexto + ">");
        }
        ConectorServicioDePersistenciaDeDatos conector = new ConectorServicioDePersistenciaDeDatos(contexto);
        try {
		    if (getLogger().isDebugEnabled()) {
                getLogger().debug("[ingresoCotizacionCliente] Parámetros para SPD: identificador=<"
                        + cotizacionCliente.getIdentificador() + ">, numeroFolio=<" 
                        + cotizacionCliente.getFolio() + ">, codigoCanal=<" + cotizacionCliente.getCanal() 
                        + ">, servicio=<"+ cotizacionCliente.getServicio() + ">, estadoRenta=<" 
                        + cotizacionCliente.getEstadoRenta() + ">, estadoError=<"
                        + cotizacionCliente.getEstadoError() + ">, renta=<" + cotizacionCliente.getMontoRenta()
                        + ">, montoRentaFija=<" +  cotizacionCliente.getMontoRentaFija() 
                        + ">, montoRentaVariable=<" + cotizacionCliente.getMontoRentaVariable() 
                        + ">, flagRegistroRTA=<0>, firma=<" + cotizacionCliente.getFirma() + ">");
		    }
			parametros.put("identificador", Integer.valueOf(
			        String.valueOf(cotizacionCliente.getIdentificador())));
            parametros.put("numeroFolio", cotizacionCliente.getFolio());
            parametros.put("codigoCanal", cotizacionCliente.getCanal());
            parametros.put("servicio", cotizacionCliente.getServicio());
            parametros.put("estadoRenta", String.valueOf(cotizacionCliente.getEstadoRenta()));
            parametros.put("estadoError", cotizacionCliente.getEstadoError());
            parametros.put("renta", new Double(cotizacionCliente.getMontoRenta()));
			parametros.put("montoRentaFija", new Double(cotizacionCliente.getMontoRentaFija()));
			parametros.put("montoRentaVariable", new Double(cotizacionCliente.getMontoRentaVariable()));
			parametros.put("flagRegistroRTA", new Integer(0));
            parametros.put("firma", cotizacionCliente.getFirma());
			if (getLogger().isDebugEnabled()) {
                getLogger().debug("[ingresoCotizacionCliente] parámetros ingresados");
            }
            registros = (Integer)conector.ejecutar("cotizacli", "grabaCotizacion", parametros);
			if (getLogger().isDebugEnabled()) {
                getLogger().debug("[ingresoCotizacionCliente] registros=<" + registros + ">");
            }
		} 
		catch (EjecutarException ejExc) {
			if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[ingresoCotizacionCliente] [EjecutarException] [BCI_FINEX] " 
                        + ejExc.getMessage(), ejExc);
            }
            throw new Exception("[ingresoCotizacionCliente]Error al relizar el ingreso"+ ejExc);
		} 
		catch (Exception e) {
			if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[ingresoCotizacionCliente] [Exception] [BCI_FINEX] "
                        + e.getMessage(), e);
            }
            throw new Exception("[ingresoCotizacionCliente]Error al relizar el ingreso"+ e);
		}
		if (getLogger().isInfoEnabled()) {
            getLogger().info("[ingresoCotizacionCliente] [BCI_FINOK] retorno=<" + registros.intValue() + ">");
        }
        return registros.intValue();
    }

    /**
     * Método permite registrar el detalle de la cotización de un cliente en particular.
     *
	 * <p>Registro de versiones:<UL>
     *  <LI>1.0 03/04/2008 Mauricio Retamal C. (SEnTRA) : versión inicial.
	 *  <li>1.1 30/07/2014 Eduardo Villagrán Morales (Imagemaker): se mejora y normaliza log
     *  <li>1.2 03/11/2014 Juan Jose Buendia (BCI): Se modifica el valor de folio de previred
	 * </UL>
     *
	 * @param cotizacionCliente contiene la información necesaria para ingresar una cotización.
	 * @param posicion posición.
	 * @return cantidad registros ingresados.
	 * @throws Exception al haber un error.
     * @since 1.0
     */
	public int ingresoDetalleCotizacionCliente(CotizacionClienteVO cotizacionCliente, int posicion) 
	        throws Exception {
	    if (getLogger().isInfoEnabled()) {
            getLogger().info("[ingresoDetalleCotizacionCliente] [BCI_INI] cotizacionCliente=<"
                    + cotizacionCliente + ">, posicion=<" + posicion + ">");
        }
        HashMap parametros = new HashMap();
        Integer registros = null;

        String contexto = TablaValores.getValor("JNDIConfig.parametros", "cluster", "param");
		if (getLogger().isDebugEnabled()) {
            getLogger().debug("[ingresoDetalleCotizacionCliente] contexto JNDI=<" + contexto + ">");
        }
        ConectorServicioDePersistenciaDeDatos conector = new ConectorServicioDePersistenciaDeDatos(contexto);
        DetalleCotizacionClienteVO[] detalleCotizacion = cotizacionCliente.getDetalleCotizacion();
        SimpleDateFormat formato = new SimpleDateFormat("ddMMyyyy", new Locale("es", "CL"));
        try {
		    if (getLogger().isDebugEnabled()) {
                getLogger().debug("[ingresoDetalleCotizacionCliente] Parámetros para SPD: numeroFolio=<"
                        + String.valueOf(cotizacionCliente.getIdentificador()) + ">, periodo=<" 
                        + detalleCotizacion[posicion].getPeriodo() + ">, remuneracionImponible=<"
                        + detalleCotizacion[posicion].getRemuneracionImponible() + ">, montoCotizado=<"
                        + detalleCotizacion[posicion].getMontoCotizacion() + ">, fechaPago=<"
                        + detalleCotizacion[posicion].getFechaPago() + ">, tipoMovimiento=<"
                        + detalleCotizacion[posicion].getTipoMovimiento() + ">, rutCliente=<"
                        + detalleCotizacion[posicion].getRutEmpleador() + ">, digitoVerificador=<"
                        + detalleCotizacion[posicion].getDvEmpleador() + ">, afp=<"
                        + detalleCotizacion[posicion].getAfp() + ">");
		    }
            parametros.put("numeroFolio", String.valueOf(cotizacionCliente.getIdentificador()));
			parametros.put("periodo", formato.parse(PRIMERDIA 
			        + detalleCotizacion[posicion].getPeriodo().trim()));
			parametros.put("remuneracionImponible", new Double(
			        detalleCotizacion[posicion].getRemuneracionImponible()));
            parametros.put("montoCotizado", new Double(detalleCotizacion[posicion].getMontoCotizacion()));
            parametros.put("fechaPago", detalleCotizacion[posicion].getFechaPago());
            parametros.put("tipoMovimiento", detalleCotizacion[posicion].getTipoMovimiento());
			parametros.put("rutCliente", Integer.valueOf(String.valueOf(
			        detalleCotizacion[posicion].getRutEmpleador())));			
            parametros.put("digitoVerificador", String.valueOf(detalleCotizacion[posicion].getDvEmpleador()));
            parametros.put("afp", detalleCotizacion[posicion].getAfp());
			if (getLogger().isDebugEnabled()) {
                getLogger().debug("[ingresoDetalleCotizacionCliente] parámetros ingresados");
            }
            registros = (Integer)conector.ejecutar("cotizacli", "grabaDetalleCotizacion", parametros);
			if (getLogger().isDebugEnabled()) {
                getLogger().debug("[ingresoDetalleCotizacionCliente] registros=<" + registros + ">");
            }
		} 
		catch (EjecutarException ejExc) {
			if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[ingresoDetalleCotizacionCliente] [EjecutarException] [BCI_FINEX] "
                        + ejExc.getMessage(), ejExc);
            }
            throw new Exception("[ingresoDetalleCotizacionCliente]Error al relizar el ingreso"+ ejExc);
		} 
		catch (Exception e) {
			if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[ingresoDetalleCotizacionCliente] [Exception] [BCI_FINEX] "
                        + e.getMessage(), e);
            }
            throw new Exception("[ingresoDetalleCotizacionCliente]Error al relizar el ingreso"+ e);
		}
		if (getLogger().isInfoEnabled()) {
            getLogger().info("[ingresoDetalleCotizacionCliente] [BCI_FINOK] retorno=<" 
                    + registros.intValue() + ">");
        }
        return registros.intValue();
    }


    /**
	 * Método que permite ingresar el error en caso de no poder consultar la o las cotizaciones de un cliente.
     *
	 * <p>Registro de versiones:<UL>
     * <li>1.0 07/04/2008, Mauricio Retamal C. (SEnTRA): versión inicial.</li>
	 * <li>1.1 30/07/2014 Eduardo Villagrán Morales (Imagemaker): se mejora y normaliza log
     * </UL>
     *
	 * @param cotizacionCliente posee la información general de la cotización.
	 * @param errorCotizacion posee el detalle del error producido al consultar la cotización.
     * @return respuesta a la operación de ingreso del error.
	 * @throws Exception al haber error.
     * @since 1.0
     */
	public int ingresaErrorCotizacion(CotizacionClienteVO cotizacionCliente, ErrorCotizacionVO errorCotizacion)
	        throws Exception  {
	    if (getLogger().isInfoEnabled()) {
            getLogger().info("[ingresaErrorCotizacion] [BCI_INI] cotizacionCliente=<" + cotizacionCliente 
                    + ">, errorCotizacion=<" + errorCotizacion + ">");
        }
        HashMap parametros = new HashMap();
        Integer registros = null;
        String contexto = TablaValores.getValor("JNDIConfig.parametros", "cluster", "param");
		if (getLogger().isDebugEnabled()) {
            getLogger().debug("[ingresaErrorCotizacion] contexto JNDI=<" + contexto + ">");
        }
        ConectorServicioDePersistenciaDeDatos conector = new ConectorServicioDePersistenciaDeDatos(contexto);
        Calendar cal = Calendar.getInstance();
        cal.setTime(cotizacionCliente.getFechaConsulta());
        try {
		    if (getLogger().isDebugEnabled()) {
                getLogger().debug("[ingresaErrorCotizacion] Parámetros SPD: indetificador=<"
                        + cotizacionCliente.getIdentificador() + ">, folio=<" 
                        + String.valueOf(cotizacionCliente.getFolio()) + ">, codigoCanal=<"
                        + cotizacionCliente.getCanal() + ">, servicio=<" + cotizacionCliente.getServicio()
                        + ">, estadoRenta=<" + cotizacionCliente.getEstadoRenta() + ">, estadoError=<"
                        + cotizacionCliente.getEstadoError() + ">, codigoError=<" 
                        + errorCotizacion.getCodigoError() + ">, detalleError=<"
                        + errorCotizacion.getDetalleError() + ">");
		    }
			parametros.put("identificador", Integer.valueOf(
			        String.valueOf(cotizacionCliente.getIdentificador())));
            parametros.put("numeroFolio", String.valueOf(cotizacionCliente.getFolio()));
            parametros.put("codigoCanal", cotizacionCliente.getCanal());
            parametros.put("servicio", cotizacionCliente.getServicio());
            parametros.put("estadoRenta", cotizacionCliente.getEstadoRenta());
            parametros.put("estadoError", cotizacionCliente.getEstadoError());
            parametros.put("codigoError", errorCotizacion.getCodigoError());
            parametros.put("detalleError", errorCotizacion.getDetalleError());
			if (getLogger().isDebugEnabled()) {
                getLogger().debug("[ingresaErrorCotizacion] parámetros ingresados.");
            }
            registros = (Integer)conector.ejecutar("cotizacli", "grabaErrorCotizacion", parametros);
			if (getLogger().isDebugEnabled()) {
                getLogger().debug("[ingresaErrorCotizacion] registros=<" + registros + ">");
            }
		} 
		catch (EjecutarException ejExc) {
		    if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[ingresaErrorCotizacion] [EjecutarException] [BCI_FINEX] "
                        + ejExc.getMessage(), ejExc);
            }
            throw new Exception("[ingresaErrorCotizacion]Error al relizar el ingreso"+ ejExc);
		} 
		catch (Exception e) {
			if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[ingresaErrorCotizacion] [Exception] [BCI_FINEX] "
                        + e.getMessage(), e);
            }
            throw new Exception("[ingresaErrorCotizacion]Error al relizar el ingreso"+ e);
		}
		if (getLogger().isInfoEnabled()) {
            getLogger().info("[ingresaErrorCotizacion] [BCI_FINOK] resultado=<" + registros.intValue() + ">");
        }
        return registros.intValue();
    }

    /**
	 * Método que permite ingresar información correspondiente al encabezado de la cotización.
     *
	 * <p>Registro de versiones:<UL>
     * <li>1.0 08/07/2008, Jaime Gaete L. (SEnTRA): versión inicial.</li>
	 * <li>1.1 30/07/2014 Eduardo Villagrán Morales (Imagemaker): se mejora y normaliza log
     * </UL>
     *
	 * @param cotizacionCliente posee la información general de la cotización.
     * @return código identificador de la cotización.
	 * @throws Exception al haber error.
     * @since 1.0
     */    
    public int ingresarEncabezadoCotizacion(CotizacionClienteVO cotizacionCliente) throws Exception{
		if (getLogger().isInfoEnabled()) {
            getLogger().info("[ingresarEncabezadoCotizacion] [BCI_INI] cotizacionCliente=<"
                    + cotizacionCliente + ">");
        }
        HashMap parametros = new HashMap();
        Integer registros = null;
        String contexto = TablaValores.getValor("JNDIConfig.parametros", "cluster", "param");
		if (getLogger().isDebugEnabled()) {
            getLogger().debug("[ingresarEncabezadoCotizacion] contexto JNDI=<" + contexto + ">");
        }		
        ConectorServicioDePersistenciaDeDatos conector = new ConectorServicioDePersistenciaDeDatos(contexto);
        Calendar cal = Calendar.getInstance();
        cal.setTime(cotizacionCliente.getFechaConsulta());    
        try {
		    if (getLogger().isDebugEnabled()) {
                getLogger().debug("[ingresarEncabezadoCotizacion] parámetros para SPD: fechaConsulta=<"
                		+ cotizacionCliente.getFechaConsulta() + ">, rutCliente=<" 
                        + cotizacionCliente.getRutCliente() + ">, digitoVerificador=<" 
                        + cotizacionCliente.getDvCliente() + ">, codigoCanal=<" +cotizacionCliente.getCanal()
                		+ ">, servicio=<" + cotizacionCliente.getServicio() + ">, estadoRenta=<"
                		+ cotizacionCliente.getEstadoRenta() + ">, estadoError=<" 
                		+ cotizacionCliente.getEstadoError() + ">" );
		    }
            parametros.put("fechaConsulta", cal);
            parametros.put("rutCliente", Integer.valueOf(String.valueOf(cotizacionCliente.getRutCliente())));
            parametros.put("digitoVerificador", String.valueOf(cotizacionCliente.getDvCliente()));
            parametros.put("codigoCanal", cotizacionCliente.getCanal());
            parametros.put("servicio", cotizacionCliente.getServicio());
            parametros.put("estadoRenta", String.valueOf(cotizacionCliente.getEstadoRenta()));
            parametros.put("estadoError", cotizacionCliente.getEstadoError());
			if (getLogger().isDebugEnabled()) {
                getLogger().debug("[ingresarEncabezadoCotizacion] parámetros ingresados");
            }
            registros = (Integer)conector.ejecutar("cotizacli", "grabaEncabezadoCotizacion", parametros);
			if (getLogger().isDebugEnabled()) {
                getLogger().debug("[ingresarEncabezadoCotizacion] registros=<" + registros + ">");
            }

		} 
		catch (EjecutarException ejExc) {
			if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[ingresarEncabezadoCotizacion] [EjecutarException] [BCI_FINEX] "
                        + ejExc.getMessage(), ejExc);
            }
            throw new Exception("[ingresaErrorCotizacion]Error al relizar el ingreso"+ ejExc);
		} 
		catch (Exception e) {
			if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[ingresarEncabezadoCotizacion] [Exception] [BCI_FINEX] "
                        + e.getMessage(), e);
            }
            throw new Exception("[ingresaErrorCotizacion]Error al relizar el ingreso"+ e);
		}
		if (getLogger().isInfoEnabled()) {
            getLogger().info("[ingresarEncabezadoCotizacion] [BCI_FINOK] retorno=<" 
                    + registros.intValue() + ">");
        }        
        return registros.intValue();
    }    

    /**
	 * Método que permite obtener el último encabezado de las cotizaciones registradas para un cliente.
     *
	 * <p>Registro de versiones:<UL>
     * <li>1.0 22/04/2009, Pedro Carmona E. (SEnTRA): versión inicial.</li>
	 * <li>1.1 30/07/2014 Eduardo Villagrán Morales (Imagemaker): se mejora y normaliza log
     * </UL>
     *
     * @param rut del cliente a consultar.
     * @param fechaValidez con fecha de validez.
     * @return cotizacionClienteVO con el último encabezado válido encontrado.
 	 * @throws Exception al haber error.
     * @since 1.1
     */    
	public CotizacionClienteVO obtieneUltimoRegistroValidoDeCotizaciones(long rut, Date fechaValidez) 
	        throws Exception{
		if (getLogger().isInfoEnabled()) {
            getLogger().info("[obtieneUltimoRegistroValidoDeCotizaciones] [" + rut 
                    + "] [BCI_INI] fechaValidez=<" + fechaValidez + ">");
        }
        HashMap parametros = new HashMap();
        List resultado = null;
        HashMap salida = null;
        CotizacionClienteVO cotizacionCliente = null;
        String contexto = TablaValores.getValor("JNDIConfig.parametros", "cluster", "param");
		if (getLogger().isDebugEnabled()) {
            getLogger().debug("[obtieneUltimoRegistroValidoDeCotizaciones] [" + rut + "] contexto JNDI=<"
                    + contexto + ">");
        }		
        
        ConectorServicioDePersistenciaDeDatos conector = new ConectorServicioDePersistenciaDeDatos(contexto);
        Calendar cal = Calendar.getInstance();
        cal.setTime(fechaValidez);    
        try {
		    if (getLogger().isDebugEnabled()) {
                getLogger().debug("[obtieneUltimoRegistroValidoDeCotizaciones] [" + rut + "] parámetros SPD: "
                        + "fechaValidez=<" + cal + ">, rutCliente=<" + rut + ">");
            }
            parametros.put("fechaValidez", cal);
            parametros.put("rutCliente", Integer.valueOf(String.valueOf(rut)));
			if (getLogger().isDebugEnabled()) {
                getLogger().debug("[obtieneUltimoRegistroValidoDeCotizaciones] [" + rut 
                        + "] parámetros ingresados");
            }
            resultado = conector.consultar("cotizacli", "obtieneEncabezadoUltimoRegistro", parametros);
			if (getLogger().isDebugEnabled()) {
                getLogger().debug("[obtieneUltimoRegistroValidoDeCotizaciones] [" + rut + "] resultado=<"
                        + resultado + ">");
            }
            if (resultado!=null && resultado.size()>0) {
                salida = (HashMap) resultado.get(0);
                cotizacionCliente = new CotizacionClienteVO();
                cotizacionCliente.setCanal(String.valueOf(salida.get("canal")));
                cotizacionCliente.setDvCliente(String.valueOf(salida.get("dv")).charAt(0));
                cotizacionCliente.setEstadoError(String.valueOf(salida.get("estadoError")));
                cotizacionCliente.setEstadoRenta(String.valueOf(salida.get("estadoRenta")));
                cotizacionCliente.setFechaConsulta(((java.util.Calendar)salida.get("fechaConsulta")).getTime());
                cotizacionCliente.setFirma("");
                cotizacionCliente.setFolio(String.valueOf(salida.get("folio")));
				cotizacionCliente.setIdentificador(
				        Integer.parseInt(String.valueOf(salida.get("identificador"))));
                double renta = 0;
                if (salida.get("remuneracionImponible")!=null) {
					 renta = Double.parseDouble(
                             String.valueOf(salida.get("remuneracionImponible")).equalsIgnoreCase("")
					         ? "0" : String.valueOf(salida.get("remuneracionImponible")));
				}
				if (getLogger().isDebugEnabled()) {
                    getLogger().debug("[obtieneUltimoRegistroValidoDeCotizaciones] [" + rut + "] renta=<"
                            + renta + ">");
                }
                cotizacionCliente.setMontoRenta(renta);
                cotizacionCliente.setRutCliente(Long.parseLong(String.valueOf(salida.get("rutCliente"))));
                cotizacionCliente.setServicio(String.valueOf(salida.get("servicio")));
            }
		} 
		catch (ConfiguracionException coExc) {
			if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[obtieneUltimoRegistroValidoDeCotizaciones] [" + rut 
                        + "] [ConfiguracionException] [BCI_FINEX] " + coExc.getMessage(), coExc);
            }
			throw new Exception("Error al relizar la consulta de encabezado ["
			        + coExc.getDetalleException() + "]");
		} 
		catch (EjecutarException ejExc) {
			if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[obtieneUltimoRegistroValidoDeCotizaciones] [" + rut 
                        + "] [EjecutarException] [BCI_FINEX] " + ejExc.getMessage(), ejExc);
            }
			throw new Exception("Error al relizar la consulta de encabezado ["
			        + ejExc.getDetalleException() + "]");
		} 
		catch (ServicioDatosException sdExc) {
			if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[obtieneUltimoRegistroValidoDeCotizaciones] [" + rut 
                        + "] [ServicioDatosException] [BCI_FINEX] " + sdExc.getMessage(), sdExc);
            }
			throw new Exception("Error al relizar la consulta de encabezado ["
			        + sdExc.getDetalleException() + "]");
		} 
		catch (Exception e) {
			if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[obtieneUltimoRegistroValidoDeCotizaciones] [" + rut 
                        + "] [Exception] [BCI_FINEX] " + e.getMessage(), e);
            }
            throw new Exception("Error al relizar la consulta de encabezado ["+ e.getMessage()+"]");
		}	
		if (getLogger().isInfoEnabled()) {
            getLogger().info("[obtieneUltimoRegistroValidoDeCotizaciones] [" + rut + "] [BCI_FINOK] retorno=<"
                    + cotizacionCliente + ">");
        }        
        return cotizacionCliente;
    }    
    
    /**
	 * Método que permite obtener el detalle de las cotizaciones relacionadas con el folio de un 
	 * encabezado en particular.
     *
	 * <p>Registro de versiones:<UL>
     * <li>1.0 22/04/2009, Pedro Carmona E. (SEnTRA): versión inicial.</li>
	 * <li>1.1 30/07/2014 Eduardo Villagrán Morales (Imagemaker): se mejora y normaliza log
     * </UL>
     *
     * @param folio con el folio que agrupa las cotizaciones.
     * @return cotizacionClienteVO[] arreglo con el detalle de cada cotizacion asociada a un folio en particular.
 	 * @throws Exception al haber error.
     * @since 1.1
     */    
    public DetalleCotizacionClienteVO[] obtieneDetalleCotizacion(String folio) throws Exception{
		if (getLogger().isInfoEnabled()) {
            getLogger().info("[obtieneDetalleCotizacion] [BCI_INI] folio=<" + folio + ">");
        }
        HashMap parametros = new HashMap();
        List resultado = null;
        HashMap salida = null;
        DetalleCotizacionClienteVO[] detalle = null;
        String contexto = TablaValores.getValor("JNDIConfig.parametros", "cluster", "param");
		if (getLogger().isDebugEnabled()) {
            getLogger().debug("[obtieneDetalleCotizacion] contexto JNDI=<" + contexto + ">");
        }		
        ConectorServicioDePersistenciaDeDatos conector = new ConectorServicioDePersistenciaDeDatos(contexto);
        try {
		    if (getLogger().isDebugEnabled()) {
                getLogger().debug("[obtieneDetalleCotizacion] parámetros SPD: folio=<" + folio + ">");
            }
            parametros.put("folio", folio);
            resultado = conector.consultar("cotizacli", "obtieneDetalleCotizaciones", parametros);
			if (getLogger().isDebugEnabled()) {
                getLogger().debug("[obtieneDetalleCotizacion] resultado=<" + resultado + ">");
            }
            if (resultado!=null && resultado.size()>0) {
                detalle = new DetalleCotizacionClienteVO[resultado.size()];
                Calendar datoCalendar=Calendar.getInstance();
                for (int i = 0 ; i< resultado.size() ; i++) {
                    salida = (HashMap) resultado.get(i);
                    detalle[i] = new DetalleCotizacionClienteVO();
                    detalle[i].setAfp(String.valueOf(salida.get("afp")));
                    detalle[i].setDvEmpleador(String.valueOf(salida.get("dvE")).charAt(0));
                    datoCalendar = (Calendar)salida.get("fechaPago");
                    detalle[i].setFechaPago(datoCalendar.getTime());
					detalle[i].setMontoCotizacion(Double.parseDouble(
					        String.valueOf(salida.get("montoCotizacion"))));
                    detalle[i].setPeriodo(String.valueOf(salida.get("periodo")));
					detalle[i].setRemuneracionImponible(Double.parseDouble(
					        String.valueOf(salida.get("remuneracion"))));
                    detalle[i].setRutEmpleador(Long.parseLong(String.valueOf(salida.get("rutE"))));
                    detalle[i].setTipoMovimiento(String.valueOf(salida.get("tipoMovimiento")));
                }
            }
		} 
		catch (ConfiguracionException coExc) {
			if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[obtieneDetalleCotizacion] [ConfiguracionException] [BCI_FINEX] "
                        + coExc.getMessage(), coExc);
            }
			throw new Exception("Error al relizar la consulta de detalle cotización ["
			        + coExc.getDetalleException() + "]");
		} 
		catch (EjecutarException ejExc) {
			if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[obtieneDetalleCotizacion] [EjecutarException] [BCI_FINEX] " 
                        + ejExc.getMessage(), ejExc);
            }
			throw new Exception("Error al relizar la consulta de detalle cotización ["
			        + ejExc.getDetalleException() + "]");
		} 
		catch (ServicioDatosException sdExc) {
			if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[obtieneDetalleCotizacion] [ServicioDatosException] [BCI_FINEX] "
                        + sdExc.getMessage(), sdExc);
            }
			throw new Exception("Error al relizar la consulta de detalle cotización ["
            + sdExc.getDetalleException() + "]");
		} 
		catch (Exception e) {
			if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[obtieneDetalleCotizacion] [Exception] [BCI_FINEX] " + e.getMessage(), e);
            }
            throw new Exception("Error al relizar la consulta de detalle cotización ["+ e.getMessage()+"]");
		}		
		if (getLogger().isInfoEnabled()) {
            getLogger().info("[obtieneDetalleCotizacion] [BCI_FINOK] retorno=<" + detalle + ">");
        }        
        return detalle;
    }    
    
    
    
    
    /**
	 * Método que permite actualizar el estado de la actualización de renta de un cliente, reflejado en el
	 * encabezado del registro de sus cotizaciones en BD.
     *
	 * <P>Registro de versiones:<UL>
     * <li>1.0 22/04/2009, Pedro Carmona E. (SEnTRA): versión inicial.</li>
	 * <li>1.1 30/07/2014 Eduardo Villagrán Morales (Imagemaker): se mejora y normaliza log
     * </UL>
     *
     * @param idRegistro con el identificador único del registro.
     * @param estado con el nuevo estado.
     * @return int que indica el resultado de la actualización.
 	 * @throws Exception al haber error.
     * @since 1.1
     */    
    public int actualizaEstadoEncabezado(int idRegistro, String estado) throws Exception{
		if (getLogger().isInfoEnabled()) {
            getLogger().info("[actualizaEstadoEncabezado] [BCI_INI] idRegistro=<" + idRegistro + ">, estado=<"
                    + estado + ">");
        }
        HashMap parametros = new HashMap();
        Integer resultado = null;
        String contexto = TablaValores.getValor("JNDIConfig.parametros", "cluster", "param");
		if (getLogger().isDebugEnabled()) {
            getLogger().debug("[actualizaEstadoEncabezado] contexto JNDI=<" + contexto + ">");
        }	
        ConectorServicioDePersistenciaDeDatos conector = new ConectorServicioDePersistenciaDeDatos(contexto);
        try {
		    if (getLogger().isDebugEnabled()) {
                getLogger().debug("[actualizaEstadoEncabezado] parámetros SPD: identificador=<"
                        + idRegistro + ">, estado=<" + estado + ">");
            }
            parametros.put("identificador", new Integer(idRegistro));
            parametros.put("estado", estado);
            resultado = (Integer) conector.ejecutar("cotizacli", "actualizaEstadoEncabezado", parametros);
        if(getLogger().isDebugEnabled()){
                getLogger().debug("[actualizaEstadoEncabezado] resultado=<" + resultado + ">");
        }
            }
		catch (ConfiguracionException coExc) {
			if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[actualizaEstadoEncabezado] [ConfiguracionException] [BCI_FINEX] "
                        + coExc.getMessage(), coExc);
                }
			throw new Exception("Error al relizar actualización de estado de encabezado ["
			        + coExc.getDetalleException() + "]");
            }
		catch (EjecutarException ejExc) {
            if(getLogger().isEnabledFor(Level.ERROR)){
                getLogger().error("[actualizaEstadoEncabezado] [EjecutarException] [BCI_FINEX] "
                        + ejExc.getMessage(), ejExc);
            }
			throw new Exception("Error al relizar actualización de estado de encabezado ["
			        + ejExc.getDetalleException() + "]");
        }        
		catch (ServicioDatosException sdExc) {
			if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[actualizaEstadoEncabezado] [ServicioDatosException] [BCI_FINEX] "
                        + sdExc.getMessage(), sdExc);
            }
            throw new Exception("Error al relizar actualización de estado de encabezado ["
			        + sdExc.getDetalleException() + "]");
		} 
		catch (Exception e) {
			if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[actualizaEstadoEncabezado] [Exception] [BCI_FINEX] " + e.getMessage(), e);
            }
			throw new Exception("Error al relizar actualización de estado de encabezado [" 
			        + e.getMessage() + "]");
		}		
		if (getLogger().isInfoEnabled()) {
            getLogger().info("[actualizaEstadoEncabezado] [BCI_FINOK] retorno=<" + resultado.intValue() + ">");
        }
		return resultado.intValue();
    }
    
    /**
     * <p>Método que permite consultar el detalle de un arriendo de bien raíz, según rut asociado.</p>
     *
     * Registro de versiones:<ul>
     *
     * <li>1.0 31/03/2014 Rodrigo Pino (SEnTRA): versión inicial.</li>
     *
     * </ul>
     *
     * @param rut rut del arrendatario.
     * @param dv dv del rut del arrendatario.
     * @param secuencia numero de secuencia del ingreso.
     * @return detalle del arriendo bien raíz.
     * @throws Exception en caso de error. 
     * @since 2.0
     */
    public DetalleArriendoTO[] consultarDetalleBienRaiz(long rut, char dv, int secuencia) throws Exception{
        
        HashMap parametros = new HashMap();
        List resultado = null;
        HashMap salida = null;
        DetalleArriendoTO[] detalle = null;
        String contexto = TablaValores.getValor("JNDIConfig.parametros", "cluster", "param");
        if(getLogger().isDebugEnabled()){
            getLogger().debug("[consultarDetalleBienRaiz] El contexto: " + contexto);        
        }
        ConectorServicioDePersistenciaDeDatos conector = new ConectorServicioDePersistenciaDeDatos(contexto);
        
        try {
            if(getLogger().isDebugEnabled()){
                getLogger().debug("[consultarDetalleBienRaiz] El rut es:["+rut+"]");
                getLogger().debug("[consultarDetalleBienRaiz] El dv es:["+dv+"]");
                getLogger().debug("[consultarDetalleBienRaiz] La secuencia es:["+secuencia+"]");
            }
            parametros.put("rut", String.valueOf(rut));
            parametros.put("secuencia", new Integer(secuencia));
            parametros.put("codigo", IDENTIFICADOR_ARRIENDO);
            
            resultado = conector.consultar(JDBC_DGC, "consultarDetalleBienRaiz", parametros);
            
            if (resultado!=null && resultado.size()>0) {
                detalle = new DetalleArriendoTO[resultado.size()];
                Calendar datoCalendar=Calendar.getInstance();
                for (int i = 0 ; i< resultado.size() ; i++) {
                    salida = (HashMap) resultado.get(i);
                    detalle[i] = new DetalleArriendoTO();
                    detalle[i].setAvaluoFiscal(Integer.parseInt(String.valueOf(salida.get("avaluoFiscal"))));
                    detalle[i].setRol(String.valueOf(salida.get("rolPropiedad")));
                    detalle[i].setRutArrendatario(Long.parseLong(String.valueOf(salida.get("rutArrendatario"))));
                    detalle[i].setDvArrendatario(RUTUtil.calculaDigitoVerificador(
                            String.valueOf(salida.get("rutArrendatario"))
                            .substring(0,String.valueOf(salida.get("rutArrendatario")).indexOf("-"))));
                }
            }
        } 
        catch (Exception e) {
            if(getLogger().isEnabledFor(Level.ERROR)){
                getLogger().error("[consultarDetalleBienRaiz] Exception: " + e.toString());
            }
            throw new Exception("Error al relizar la consulta de detalle bien raíz ["+ e.getMessage()+"]");
        }        
        return detalle;
        
    }

/**
     * <p>Método que permite ingrsar el detalle de bien raíz, según el rut asociado.</p>
     *
     * Registro de versiones:<ul>
     *
     * <li>1.0 15/04/2014 Rodrigo Pino (SEnTRA): versión inicial.</li>
     *
     * </ul>
     *
     * @param rut rut del cliente.
     * @param dv digito verificador del cliente.
     * @param secuencia numero de secuencia del ingreso.
     * @param detalleArriendoTO TO que contiene el detalle del arriendo.
     * @return respuesta de ingreso correcto.
     * @throws Exception excepcion.
     * @since 2.0
     */
    public boolean ingresarDetalleBienRaiz(long rut, char dv, int secuencia, DetalleArriendoTO detalleArriendoTO) 
            throws Exception{
    
        if(getLogger().isDebugEnabled()){
            getLogger().debug("[ingresarDetalleBienRaiz] Inicio método");
        }
        HashMap parametros = new HashMap();
        
        //RESULTADO PUESTO EN TRUE PARA PRUEBA, CAMBIAR.
        boolean resultado = false;
        
        HashMap salida = null;
        String contexto = TablaValores.getValor("JNDIConfig.parametros", "cluster", "param");
        if(getLogger().isDebugEnabled()){
            getLogger().debug("[obtieneDetalleCotizacion] El contexto: " + contexto);        
        }
        ConectorServicioDePersistenciaDeDatos conector = new ConectorServicioDePersistenciaDeDatos(contexto);
        
        try {
            if(getLogger().isDebugEnabled()){
                getLogger().debug("[ingresarDetalleBienRaiz] El rut es:["+rut+"]");
                getLogger().debug("[ingresarDetalleBienRaiz] El dv es:["+dv+"]");
                getLogger().debug("[ingresarDetalleBienRaiz] La secuencia es:["+secuencia+"]");
                getLogger().debug("[ingresarDetalleBienRaiz] avaluoFiscal:["
                +detalleArriendoTO.getAvaluoFiscal()+"]");
                getLogger().debug("[ingresarDetalleBienRaiz] rutArrendatario:["
                +detalleArriendoTO.getRutArrendatario()+"]");
                getLogger().debug("[ingresarDetalleBienRaiz] dvArrendatario:["
                +detalleArriendoTO.getDvArrendatario()+"]");
                getLogger().debug("[ingresarDetalleBienRaiz] rolBienRaiz:["+detalleArriendoTO.getRol()+"]");
            }
            String rutArrendatario = String.valueOf(detalleArriendoTO.getRutArrendatario()) 
                    + "-" + String.valueOf(detalleArriendoTO.getDvArrendatario());
            
            parametros.put("rut", String.valueOf(rut));
            parametros.put("dv", String.valueOf(dv));
            parametros.put("secuencia", new Integer(secuencia));
            parametros.put("codigo", IDENTIFICADOR_ARRIENDO);
            parametros.put("avaluoFiscal", new Integer(detalleArriendoTO.getAvaluoFiscal()));
            parametros.put("rolBienRaiz", detalleArriendoTO.getRol());
            parametros.put("rutArrendatario", rutArrendatario);
            
            conector.ejecutar(JDBC_DGC, "ingresaDetalleBienRaiz", parametros);
            resultado = true;

        } 
        catch (Exception e) {
            if(getLogger().isEnabledFor(Level.ERROR)){
                getLogger().error("[ingresarDetalleBienRaiz] Exception: " + e.toString());
            }
            resultado = false;
            throw new Exception("Error al ingresar detalle de bien raiz ["+ e.getMessage()+"]");
        }    
        return resultado;
 
    }
    
    /**
     * <p>Método que retorna la variable logger de la clase.</p>
     * 
     * Registro de Versiones:
     * <ul>
     * <li>1.0 15/04/2014 Pedro Rebolledo L. (SEnTRA): versión inicial.</li>
     * </ul>
     * 
     * @return Logger Variable logger de la clase.
     * @since 2.0
     */
    public Logger getLogger(){
        if (logger == null){
            logger = Logger.getLogger(this.getClass());
        }
        return logger;
    }

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
     * @throws Exception de la eliminación.
     * @since 2.0
     */
    public void eliminarBorradorRenta(CalculoRentaTO calculoRenta) throws Exception{
        if (getLogger().isDebugEnabled()){
            getLogger().debug("[eliminarBorradorRenta][BCI_INI]");
        }
        HashMap parametros = new HashMap();
        String contexto = TablaValores.getValor("JNDIConfig.parametros", "cluster", "param");
     
        parametros.put("rutCli", calculoRenta.getEvaluacionCliente().getRutCliente());
        parametros.put("rtaSec", new Integer(calculoRenta.getEvaluacionCliente().getNumeroSecuencia()));
        if (getLogger().isDebugEnabled()){
            getLogger().debug("[eliminarBorradorRenta] contexto " + contexto 
                    + " rutCliente() " + calculoRenta.getEvaluacionCliente().getRutCliente()
                    + " numeroSecuencia() " + calculoRenta.getEvaluacionCliente().getNumeroSecuencia());
        }
        ConectorServicioDePersistenciaDeDatos conector = new ConectorServicioDePersistenciaDeDatos(contexto);
        try{
            conector.ejecutar("dgc", "eliminarBorradorRenta", parametros);
        }
        catch(Exception e){
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[eliminarBorradorRenta] [BCI_FINEX]Ha ocurrido un error durante el "
                    + "proceso de eliminar Borrador Renta" + ErroresUtil.extraeStackTrace(e));
            }
            throw e;
            
        }
        
    }

	/**
     * Inserta registro en la RTA.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 25/08/2014 Manuel Escárate (Bee S.A.): Version inicial.</li>
     * <li>1.1 25/02/2015 Manuel Escárate (BEE): Se agregan campos en la inserción a rta </li>
     * </ul>
     * @param cotizacionCliente renta del cliente.                                                  
     * @throws Exception En caso de un error.
     * @return int resultado.
     * @since 1.0
     */
	public int ingresarRegistroRTA(CotizacionClienteVO cotizacionCliente) throws Exception{
    	if(getLogger().isDebugEnabled()){
    		getLogger().debug("[ingresarRegistroRTA] Inicio [BCI_INI]");
    	}
        HashMap parametros = new HashMap();
        parametros.put("CHAINED_SUPPORT", "true");
        Integer resultado = null;
        String contexto = TablaValores.getValor("JNDIConfig.parametros", "cluster", "param");
        if(getLogger().isDebugEnabled()){
            getLogger().debug("[ingresarRegistroRTA] El contexto: " + contexto);
            getLogger().debug("[ingresarRegistroRTA] Llamada al procedimiento sp_ing_usu_rta");
        }
        ConectorServicioDePersistenciaDeDatos conector = new ConectorServicioDePersistenciaDeDatos(contexto);
        try {
            String origenAct = TablaValores
                    .getValor(TABLA_PARAMETROS_CALCULORENTA, "origenAct", "valor");
            String usuarioIngreso = TablaValores
                    .getValor(TABLA_PARAMETROS_CALCULORENTA, "usuarioIngreso", "valor");
            String usuarioActualizacion = TablaValores
                    .getValor(TABLA_PARAMETROS_CALCULORENTA, "usuarioActualizacion", "valor");
            double rentaPreviredFijaRedon = DoubleUtl.redondea(
                    (cotizacionCliente.getMontoRentaFija()/(double)MONTO_MULTI_RENTA),0);    
            double rentaPreviredVariableRedon = DoubleUtl.redondea(
                    (cotizacionCliente.getMontoRentaVariable()/(double)MONTO_MULTI_RENTA),0);  
            
            double rentaPreviredFija = rentaPreviredFijaRedon*MONTO_MULTI_RENTA;
            double rentaPreviredVariable = rentaPreviredVariableRedon*MONTO_MULTI_RENTA;
            String periodoCotiza  = cotizacionCliente.getDetalleCotizacion()[
                                              cotizacionCliente.getDetalleCotizacion().length-1].getPeriodo();
            String dia = periodoCotiza.substring(0, PRIMER_VALOR_SUB);
            String anio = periodoCotiza.substring(PRIMER_VALOR_SUB, SEGUNDO_VALOR_SUB);
            String periodoPrevired = anio+dia;
            parametros.put("rut", String.valueOf(cotizacionCliente.getRutCliente()));
            parametros.put("digitoVerif", String.valueOf(cotizacionCliente.getDvCliente()));
            parametros.put("canal", cotizacionCliente.getCanal());
            parametros.put("origen", origenAct);
            parametros.put("usuarioIngreso",usuarioIngreso);
            parametros.put("usuarioAct", usuarioActualizacion);
            parametros.put("rentaFijaCalculada", new Double(rentaPreviredFija));
            parametros.put("rentaVariableCalculada", new Double(rentaPreviredVariable));
            parametros.put("ultimoPeriodo", periodoPrevired+periodoPrevired);
            resultado = (Integer) conector.ejecutar("dgc", "ingresoRegistroRTA", parametros);
        } 
        catch (ConfiguracionException coExc) {
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[ingresarRegistroRTA] ["
                        + "ConfiguracionException] [BCI_FINEX] " + coExc.getMessage(), coExc);
            }
            throw new Exception("Error al insertar en RTA ["
                    + coExc.getDetalleException() + "]");
        } 
        catch (EjecutarException ejExc) {
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[ingresarRegistroRTA] "
                        + "[EjecutarException] [BCI_FINEX] " + ejExc.getMessage(), ejExc);
            }
            throw new Exception("Error al insertar en RTA  ["
                    + ejExc.getDetalleException() + "]");
        } 
        catch (ServicioDatosException sdExc) {
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[ingresarRegistroRTA] "
                        + "[ServicioDatosException] [BCI_FINEX] " + sdExc.getMessage(), sdExc);
            }
            throw new Exception("Error al insertar en RTA  ["
                    + sdExc.getDetalleException() + "]");
        } 
        catch (Exception e) {
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[ingresarRegistroRTA]"
                        + " [Exception] [BCI_FINEX] " + e.getMessage(), e);
            }
            throw new Exception("Error al insertar en RTA  ["+ e.getMessage()+"]");
        }    
        return resultado.intValue();
    }  
 
	/**
     * Método que graba el encabezado del cálculo automático.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 19/03/2015 Alejandro Barra (SEnTRA) - Andrés Alvarado (Ing. Soft. BCI): versión inicial.
     * </ul>
     * <p>
     * 
     * @param encabezadoCalculoAutomatico EncabezadoCalculoAutomaticoTO.
     * @return ResultadoRegistroRentaTO resultadoRegistroRenta.
     * @throws Exception en caso de haber algun error.
     * @since 3.0
     */
    public ResultadoRegistroRentaTO grabaEncabezadoCalculoAutomatico(EncabezadoCalculoAutomaticoTO 
                    encabezadoCalculoAutomatico) throws Exception{
        
        if (getLogger().isEnabledFor(Level.INFO)){
            getLogger().info("[grabaEncabezadoCalculoAutomatico][BCI_INI]");
        }
        int respuesta = -1;
        HashMap parametros = new HashMap();
        String contexto = TablaValores.getValor("JNDIConfig.parametros", "cluster", "param");
        parametros.put("CHAINED_SUPPORT", "true");
        parametros.put("secuencia",  new Integer(encabezadoCalculoAutomatico.getSecuencia()));
        parametros.put("fechaRegistro", encabezadoCalculoAutomatico.getFechaRegistro());
        parametros.put("fechaCertificado", encabezadoCalculoAutomatico.getFechaCertificado());
        parametros.put("codigoCertificado", encabezadoCalculoAutomatico.getCodigoCertificado());
        parametros.put("rutCertificado", new Long(encabezadoCalculoAutomatico.getRutCertificado()));
        parametros.put("dvCertificado", String.valueOf(encabezadoCalculoAutomatico.getDvCertificado()));
        parametros.put("usuarioRegistro", encabezadoCalculoAutomatico.getUsuarioRegistro());
        parametros.put("afp", encabezadoCalculoAutomatico.getAfpCertificado());
        
        ConectorServicioDePersistenciaDeDatos conector = new ConectorServicioDePersistenciaDeDatos(contexto);
        ResultadoRegistroRentaTO resultadoRegistroRenta = new ResultadoRegistroRentaTO();
        resultadoRegistroRenta.setCorrelativo(respuesta);
        try{
            conector.ejecutar("dgc", "grabaEncabezadoCalculoAutomatico", parametros);
            resultadoRegistroRenta.setCorrelativo(1);
            resultadoRegistroRenta.setResultado(true);
        }
        catch(Exception e){
            if (getLogger().isEnabledFor(Level.ERROR)){
                getLogger().error("[grabaEncabezadoCalculoAutomatico][BCI_FINEX][Exception] error"
                + " con mensaje: " + e.getMessage(), e);
            }
            throw e;
            
        }
        
        if (getLogger().isEnabledFor(Level.INFO)){
            getLogger().info("[grabaEncabezadoCalculoAutomatico][BCI_FINOK] retornando resultadoRegistroRenta "
                             + resultadoRegistroRenta);
        }
        return resultadoRegistroRenta;
    }
    
     /**
     * Método que graba el detalle del cálculo automático.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 19/03/2015 Alejandro Barra (SEnTRA) - Andrés Alvarado (Ing. Soft. BCI): versión inicial.
     * </ul>
     * <p>
     * 
     * @param detalleCotizacionCliente DetalleCotizacionClienteVO.
     * @param encabezadoCalculoAutomatico EncabezadoCalculoAutomaticoTO.
     * @return ResultadoRegistroRentaTO resultadoRegistroRenta.
     * @throws Exception en caso de haber algun error.
     * @since 3.0
     */
    public ResultadoRegistroRentaTO grabaDetallesCalculoAutomatico(
                    DetalleCotizacionClienteVO detalleCotizacionCliente, 
                    EncabezadoCalculoAutomaticoTO encabezadoCalculoAutomatico) throws Exception {
        
        if (getLogger().isEnabledFor(Level.INFO)){
            getLogger().info("[grabaDetallesCalculoAutomatico][BCI_INI]");
        }
        int respuesta = -1;
        HashMap parametros = new HashMap();
        String contexto = TablaValores.getValor("JNDIConfig.parametros", "cluster", "param");
        parametros.put("CHAINED_SUPPORT", "true");
        parametros.put("secuencia",  new Integer(encabezadoCalculoAutomatico.getSecuencia()));
        parametros.put("periodoCotizado", detalleCotizacionCliente.getPeriodo());
        parametros.put("rutEmpleador", new Long(detalleCotizacionCliente.getRutEmpleador()));
        parametros.put("dvEmpleador", String.valueOf(detalleCotizacionCliente.getDvEmpleador()));
        parametros.put("montoCotizado", new BigDecimal(detalleCotizacionCliente.getMontoCotizacion()));
        parametros.put("fechaPagoCotizacion", detalleCotizacionCliente.getFechaPago());
        
        
        ConectorServicioDePersistenciaDeDatos conector = new ConectorServicioDePersistenciaDeDatos(contexto);
        ResultadoRegistroRentaTO resultadoRegistroRenta = new ResultadoRegistroRentaTO();
        resultadoRegistroRenta.setCorrelativo(respuesta);
        try{
            conector.ejecutar("dgc", "grabaDetallesCalculoAutomatico", parametros);
            resultadoRegistroRenta.setCorrelativo(0);
            resultadoRegistroRenta.setResultado(true);
            
        }
        catch(Exception e){
            if (getLogger().isEnabledFor(Level.ERROR)){
                getLogger().error("[grabaDetallesCalculoAutomatico][BCI_FINEX][Exception] error"
                + " con mensaje: " + e.getMessage(), e);
            }
            throw e;
        }
        
        if (getLogger().isEnabledFor(Level.INFO)){
            getLogger().info("[grabaDetallesCalculoAutomatico][BCI_FINOK] retornando resultadoRegistroRenta "
                             + resultadoRegistroRenta);
        }
        return resultadoRegistroRenta;
        
    }
    
     /**
     * Método que consulta el encabezado del cálculo automático
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 19/03/2015 Alejandro Barra (SEnTRA) - Andrés Alvarado (Ing. Soft. BCI): versión inicial.
     * </ul>
     * <p>
     * 
     * @param secuencia int.
     * @return resultadoRegistroRenta EncabezadoCalculoAutomaticoTO.
     * @throws Exception en caso de haber algun error.
     * @since 3.0
     */
    public EncabezadoCalculoAutomaticoTO consultaEncabezadoCalculoAutomatico(int secuencia) throws Exception {
        
        if (getLogger().isEnabledFor(Level.INFO)){
            getLogger().info("[consultaEncabezadoCalculoAutomatico][BCI_INI] inicio secuencia[" + secuencia + "]");
        }
        int respuesta = -1;
        List registros = null; 
        HashMap salida = null; 
        HashMap parametros = new HashMap();
        String contexto = TablaValores.getValor("JNDIConfig.parametros", "cluster", "param");
        parametros.put("CHAINED_SUPPORT", "true");
        parametros.put("secuencia",  new Integer(secuencia));
        
        EncabezadoCalculoAutomaticoTO resultadoRegistroRenta = null;
        
        try{
            ConectorServicioDePersistenciaDeDatos conector = new ConectorServicioDePersistenciaDeDatos(contexto);
            registros = conector.consultar("dgc","consultaEncabezadoCalculoAutomatico", parametros);
            
            if (registros != null ){
                if (getLogger().isEnabledFor(Level.DEBUG)){
                    getLogger().debug("Cantidad de registros retornados: [" + registros.size() + "]");
                }
                   resultadoRegistroRenta = new EncabezadoCalculoAutomaticoTO();  
                   salida = (HashMap)registros.get(0);
                   resultadoRegistroRenta.setSecuencia((((Integer)salida.get("secuencia")).intValue()));
                   if (getLogger().isEnabledFor(Level.DEBUG)){
                       getLogger().debug("getSecuencia[" + resultadoRegistroRenta.getSecuencia() +"]");
                   }
                   resultadoRegistroRenta.setFechaRegistro((Date)(salida.get("fechaRegistro")));
                   if (getLogger().isEnabledFor(Level.DEBUG)){
                       getLogger().debug("getFechaRegistro["+ resultadoRegistroRenta.getFechaRegistro() +"]");
                   }
                   resultadoRegistroRenta.setFechaCertificado((Date)salida.get("fechaCertificado"));
                   if (getLogger().isEnabledFor(Level.DEBUG)){
                       getLogger().debug("getFechaCertificado["
                       + resultadoRegistroRenta.getFechaCertificado() +"]");
                   }
                   resultadoRegistroRenta.setCodigoCertificado((String)salida.get("codigoCertificado"));
                   if (getLogger().isEnabledFor(Level.DEBUG)){
                       getLogger().debug("getCodigoCertificado      ["
                       + resultadoRegistroRenta.getCodigoCertificado() +"]");
                   }
                   resultadoRegistroRenta.setRutCertificado(((Long)salida.get("rutCertificado")).longValue());
                   if (getLogger().isEnabledFor(Level.DEBUG)){
                       getLogger().debug("getRutCertificado      ["
                       + resultadoRegistroRenta.getRutCertificado() +"]");
                   }
                  resultadoRegistroRenta.setDvCertificado((String.valueOf(salida.get("dvCertificado")).charAt(0)));
                   if (getLogger().isEnabledFor(Level.DEBUG)){
                       getLogger().debug("getDvCertificado      ["
                       + resultadoRegistroRenta.getDvCertificado() +"]");
                   }
                   resultadoRegistroRenta.setUsuarioRegistro((String)salida.get("usuarioRegistrado"));
                   if (getLogger().isEnabledFor(Level.DEBUG)){
                       getLogger().debug("getUsuarioRegistro      ["
                       + resultadoRegistroRenta.getUsuarioRegistro() +"]");
                   }
                   resultadoRegistroRenta.setAfpCertificado((String)salida.get("certificadoAFP"));
                   if (getLogger().isEnabledFor(Level.DEBUG)){
                       getLogger().debug("getAFPCertificado      ["
                       + resultadoRegistroRenta.getAfpCertificado() +"]");
                   }
             }
        }
        catch(Exception e){
            if (getLogger().isEnabledFor(Level.ERROR)){
                getLogger().error("[consultaEncabezadoCalculoAutomatico][BCI_FINEX][Exception] error"
                + " con mensaje: " + e.getMessage(), e);
            }
            throw e;
            
        }
        
        if (getLogger().isEnabledFor(Level.INFO)){
            getLogger().info("[consultaEncabezadoCalculoAutomatico][BCI_FINOK] retornando resultadoRegistroRenta "
                             + resultadoRegistroRenta);
        }
        return resultadoRegistroRenta;
    }
    
     /**
     * Método que consulta el detalle del cálculo automático
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 19/03/2015 Alejandro Barra (SEnTRA) - Andrés Alvarado (Ing. Soft. BCI): versión inicial.
     * </ul>
     * <p>
     * 
     * @param secuencia int.
     * @return resultadoRegistroRenta DetalleCotizacionClienteVO[].
     * @throws Exception en caso de haber algun error.
     * @since 3.0
     */
    public DetalleCotizacionClienteVO[] consultaDetallesCalculoAutomatico(int secuencia) throws Exception {
        
        if (getLogger().isEnabledFor(Level.INFO)){
            getLogger().info("[consultaDetallesCalculoAutomatico][BCI_INI] inicio secuencia[" + secuencia + "]");
        }
        int respuesta = -1;
        List registros = null; 
        HashMap salida = null; 
        HashMap parametros = new HashMap();
        String contexto = TablaValores.getValor("JNDIConfig.parametros", "cluster", "param");
        parametros.put("CHAINED_SUPPORT", "true");
        parametros.put("secuencia",  new Integer(secuencia));
        
        DetalleCotizacionClienteVO[] resultadoRegistroRenta = null;
        
        try{
            ConectorServicioDePersistenciaDeDatos conector = new ConectorServicioDePersistenciaDeDatos(contexto);
            registros = conector.consultar("dgc","consultaDetallesCalculoAutomatico", parametros);
            
            if (registros != null ){
                if (getLogger().isEnabledFor(Level.DEBUG)){
                    getLogger().debug("Cantidad de registros retornados: [" 
                      + registros.size() + "]");
                }
                   resultadoRegistroRenta = new DetalleCotizacionClienteVO[registros.size()];  
                   
                   for(int i = 0; i < registros.size() ;i++){
                       salida = (HashMap)registros.get(i);
                       resultadoRegistroRenta[i] = new DetalleCotizacionClienteVO();
                       
                       resultadoRegistroRenta[i].setPeriodo((String)salida.get("periodoCotizado"));
                       if (getLogger().isEnabledFor(Level.DEBUG)){
                           getLogger().debug("getPeriodo      ["
                           + resultadoRegistroRenta[i].getPeriodo() +"]");
                       }
                       resultadoRegistroRenta[i].setRutEmpleador(((Long)salida.get("rutEmpleador")).longValue());
                       if (getLogger().isEnabledFor(Level.DEBUG)){
                           getLogger().debug("getRutEmpleador      ["
                           + resultadoRegistroRenta[i].getRutEmpleador() +"]");
                       }
                       resultadoRegistroRenta[i].setDvEmpleador(((String)salida.get("dvEmpleador")).charAt(0));
                       if (getLogger().isEnabledFor(Level.DEBUG)){
                           getLogger().debug("getDvEmpleador      ["
                           + resultadoRegistroRenta[i].getDvEmpleador() +"]");
                       }
                       resultadoRegistroRenta[i].setMontoCotizacion(((Double)salida.get("montoCotizado"))
                           .doubleValue());
                       if (getLogger().isEnabledFor(Level.DEBUG)){
                           getLogger().debug("getMontoCotizacion()      ["
                           + resultadoRegistroRenta[i].getMontoCotizacion() +"]");
                       }
                       resultadoRegistroRenta[i].setFechaPago((Date)salida.get("fechaPago"));
                       if (getLogger().isEnabledFor(Level.DEBUG)){
                           getLogger().debug("getFechaPago      ["
                           + resultadoRegistroRenta[i].getFechaPago() +"]");
                       }
                   }
             }
        }
        catch(Exception e){
            if(getLogger().isEnabledFor(Level.ERROR)){
                getLogger().error("[consultaDetallesCalculoAutomatico][BCI_FINEX][Exception] error"
                + " con mensaje: " + e.getMessage(), e);
            }
            throw e;
        }
        
        if(getLogger().isEnabledFor(Level.INFO)){
            getLogger().info("[consultaDetallesCalculoAutomatico][BCI_FINOK] retornando resultadoRegistroRenta "
                             + resultadoRegistroRenta);
        }
        return resultadoRegistroRenta;
    }
    
    /**
     * Método que ingresa encabezado de cotización para un prospecto.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 15/03/2016 Andrés Cea S. (TINet) - Oliver Hidalgo (Ing. Soft. BCI): Versión inicial.</li>
     * </ul>
     * </p>
     * @param renta TO renta con la información de encabezado a insertar en bdd.
     * @return identificador de registro insertado.
     * @throws GeneralException en caso de haber error en la inserción.
     * @since 3.1
     */
    public int ingresarEncabezadoCotizacionProspecto(RentaTO renta) throws GeneralException {
        if (getLogger().isInfoEnabled()) {
            getLogger().info(
                "[ingresarEncabezadoCotizacionProspecto] [BCI_INI] [" + renta.getRut() + "]  rentaTO=" + renta);
        }
        HashMap parametros = new HashMap();
        Integer idRegistro = null;
        try {
            String contexto = TablaValores.getValor("JNDIConfig.parametros", "cluster", "param");
            if (getLogger().isDebugEnabled()) {
                getLogger().debug(
                    "[ingresarEncabezadoCotizacionProspecto] [" + renta.getRut() + "] contexto JNDI=" + contexto);
            }
            ConectorServicioDePersistenciaDeDatos conector = new ConectorServicioDePersistenciaDeDatos(contexto);

            if (getLogger().isDebugEnabled()) {
                getLogger().debug(
                    "[ingresarEncabezadoCotizacionProspecto] fechaConsulta=" + renta.getFechaActualizacion());
                getLogger().debug("[ingresarEncabezadoCotizacionProspecto] rutCliente=" + renta.getRut());
                getLogger().debug("[ingresarEncabezadoCotizacionProspecto] dv=" + renta.getDv());
                getLogger().debug("[ingresarEncabezadoCotizacionProspecto] canal=" + renta.getCanal());
                getLogger().debug("[ingresarEncabezadoCotizacionProspecto] servicio=" + renta.getServicio());
                getLogger().debug("[ingresarEncabezadoCotizacionProspecto] estado=" + renta.getEstado());
            }

            parametros.put("rutCliente", Integer.valueOf(String.valueOf(renta.getRut())));
            parametros.put("codigoViaje", Integer.valueOf(String.valueOf(renta.getCodigoViaje())));
            parametros.put("fechaConsulta", renta.getFechaActualizacion());
            parametros.put("estadoRenta", renta.getEstado());

            if (getLogger().isDebugEnabled()) {
                getLogger().debug("[ingresarEncabezadoCotizacionProspecto] [" + renta.getRut() + "] "
                    + "parámetros=" + parametros);
            }
            idRegistro = (Integer) conector.ejecutar("prospectos", "grabaEncabezadoCotizacion", parametros);
            if (getLogger().isDebugEnabled()) {
                getLogger().debug(
                    "[ingresarEncabezadoCotizacionProspecto] [" + renta.getRut() + "] idRegistro= " + idRegistro);
            }
        }
        catch (Exception e) {
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[ingresarEncabezadoCotizacionProspecto] [Exception] [BCI_FINEX] ["
                    + renta.getRut() + "]" + "Error al relizar el ingreso");
            }
            throw new GeneralException(ERROR_UPDATE_CABECERA);
        }
        if (getLogger().isInfoEnabled()) {
            getLogger().info("[ingresarEncabezadoCotizacionProspecto] [BCI_FINOK] [" + renta.getRut() + "] "
                + "idRegistro=" + idRegistro.intValue());
        }
        return idRegistro.intValue();
    }
    
    
    /**
     * Método que actualiza el estado del encabezado de cotización para un prospecto.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 15/03/2016 Andrés Cea S. (TINet) - Oliver Hidalgo (Ing. Soft. BCI): Versión inicial.</li>
     * </ul>
     * </p>
     * @param idRegistro identificador del encabezado a actualizar
     * @param estado nuevo estado del encabezado a actualizar.
     * @throws GeneralException en caso de haber error en la actualización.
     * @since 3.1
     */
    public void actualizarEstadoEncabezadoCotizacionProspecto(int idRegistro, char estado)
        throws GeneralException {
        if (getLogger().isInfoEnabled()) {
            getLogger().info("[actualizarEstadoEncabezadoCotizacionProspecto] [BCI_INI] idRegistro=" + idRegistro);
            getLogger().info("[actualizarEstadoEncabezadoCotizacionProspecto] [BCI_INI] estado=" + estado);
        }
        try {
            HashMap parametros = new HashMap();
            String contexto = TablaValores.getValor("JNDIConfig.parametros", "cluster", "param");
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("[actualizarEstadoEncabezadoCotizacionProspecto] contexto JNDI=" + contexto);
            }
            ConectorServicioDePersistenciaDeDatos conector = new ConectorServicioDePersistenciaDeDatos(contexto);

            if (getLogger().isDebugEnabled()) {
                getLogger()
                    .info("[actualizarEstadoEncabezadoCotizacionProspecto] [BCI_INI] idRegistro=" + idRegistro);
                getLogger().info("[actualizarEstadoEncabezadoCotizacionProspecto] [BCI_INI] estado=" + estado);
            }
            parametros.put("identificador", new Integer(idRegistro));
            parametros.put("estado", String.valueOf(estado));

            if (getLogger().isDebugEnabled()) {
                getLogger().debug("[actualizarEstadoEncabezadoCotizacionProspecto] parámetros=" + parametros);
            }
            conector.ejecutar("prospectos", "actualizaEstadoEncabezadoCotizacion", parametros);
        }
        catch (Exception e) {
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[actualizarEstadoEncabezadoCotizacionProspecto] [BCI_FINEX] "
                    + "Error al relizar la actualización", e);
            }
            throw new GeneralException(ERROR_UPDATE_ESTADO_CABECERA);
        }
        if (getLogger().isInfoEnabled()) {
            getLogger().info("[actualizarEstadoEncabezadoCotizacionProspecto] [BCI_FINOK] Actualización OK");
        }
    }
    
    
    /**
     * Método que ingresa el detalle de las cotizaciones de un prospecto.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 15/03/2016 Andrés Cea S. (TINet) - Oliver Hidalgo (Ing. Soft. BCI): Versión inicial.</li>
     * </ul>
     * </p>
     * @param renta TO con arreglo de detalle de las cotizaciones a insertar.
     * @return número de registros insertados.
     * @throws GeneralException en caso de haber error en la inserción.
     * @since 3.1
     */
    public int ingresoDetalleCotizacionProspecto(RentaTO renta) throws GeneralException {
        if (getLogger().isEnabledFor(Level.INFO)) {
            getLogger().info("[ingresoDetalleCotizacionProspecto] [BCI_INI] Inicio");
        }
        if (renta == null) {
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[ingresoDetalleCotizacionProspecto] [GeneralException] [BCI_FINEX] renta nula");
            }
            throw new GeneralException(ERROR_INSERT_COTIZACIONES);
        }
        if (getLogger().isEnabledFor(Level.DEBUG)) {
            getLogger().debug("[ingresoDetalleCotizacionProspecto] rentaTO=" + renta);
        }
        int cantidadRegistrosInsertados = 0;
        try {
            HashMap parametros = null;

            String contexto = TablaValores.getValor("JNDIConfig.parametros", "cluster", "param");
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("[ingresoDetalleCotizacionProspecto] contexto JNDI=" + contexto);
            }
            ConectorServicioDePersistenciaDeDatos conector = new ConectorServicioDePersistenciaDeDatos(contexto);
            CotizacionTO[] cotizaciones = renta.getCotizaciones();

            if (cotizaciones != null) {
                for (int i = 0; i < cotizaciones.length; i++) {
                    if (getLogger().isInfoEnabled()) {
                        getLogger().info("[ingresoDetalleCotizacionProspecto] [BCI_INI] cotizacion[" + i + "] ="
                            + cotizaciones[i]);
                    }
                    parametros = new HashMap();
                    parametros.put("idEncabezado", new Integer(renta.getIdentificador()));
                    parametros.put("periodo", cotizaciones[i].getMes());
                    parametros.put("remuneracionImponible",
                        new Double(cotizaciones[i].getRemuneracionImponible()));
                    parametros.put("montoCotizado", new Double(cotizaciones[i].getMonto()));
                    parametros.put("fechaPago", cotizaciones[i].getFechaPago());
                    parametros.put("tipoMovimiento", cotizaciones[i].getTipoMovimiento());
                    parametros.put("rutEmpleador",
                        Integer.valueOf(String.valueOf(cotizaciones[i].getRutEmpleador())));
                    parametros.put("digitoVerificador", String.valueOf(cotizaciones[i].getDvEmpleador()));
                    parametros.put("afp", cotizaciones[i].getAfp());
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("[ingresoDetalleCotizacionProspecto] parámetros=" + parametros);
                    }
                    Integer idRegistro = (Integer) conector.ejecutar("prospectos", "grabaDetalleCotizacion",
                        parametros);
                    if (getLogger().isDebugEnabled()) {
                        getLogger()
                            .debug("[ingresoDetalleCotizacionProspecto] idRegistroCotizacion=" + idRegistro);
                    }
                    cantidadRegistrosInsertados++;
                }
            }
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("[ingresoDetalleCotizacionProspecto] cantidadRegistrosInsertados="
                    + cantidadRegistrosInsertados);
            }
        }
        catch (Exception e) {
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[ingresoDetalleCotizacionProspecto] [Exception] [BCI_FINEX] " + e.getMessage(),
                    e);
            }
            throw new GeneralException(ERROR_INSERT_COTIZACIONES);
        }
        if (getLogger().isEnabledFor(Level.INFO)) {
            getLogger().info("[ingresoDetalleCotizacionProspecto] [BCI_FINOK] cantidadRegistrosInsertados="
                + cantidadRegistrosInsertados);
        }
        return cantidadRegistrosInsertados;
    }
    
    /**
     * Método que ingresa la renta calculada actualizada para un prospecto.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 15/03/2016 Andrés Cea S. (TINet) - Oliver Hidalgo (Ing. Soft. BCI): Versión inicial.</li>
     * </ul>
     * </p>
     * @param renta TO con datos de la rena actualizada.
     * @return identificador del registro de renta insertado.
     * @throws Exception en caso de haber error en la inserción.
     * @since 3.1
     */
    public int ingresarRentaCalculadaProspecto(RentaTO renta) throws Exception {
        if (getLogger().isInfoEnabled()) {
            getLogger().info("[ingresarRentaCalculadaProspecto] [BCI_INI] rentaTO=" + renta);
        }
        HashMap parametros = new HashMap();
        Integer idRegistro = null;
        String contexto = TablaValores.getValor("JNDIConfig.parametros", "cluster", "param");
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("[ingresarRentaCalculadaProspecto] contexto JNDI=" + contexto);
        }
        ConectorServicioDePersistenciaDeDatos conector = new ConectorServicioDePersistenciaDeDatos(contexto);
        try {
            parametros.put("rut", new Integer(String.valueOf(renta.getRut())));
            parametros.put("rentaFijaCalculada", new Double(renta.getRentaFija()));
            parametros.put("rentaVariableCalculada", new Double(renta.getRentaVariable()));
            parametros.put("rentaLiquidaCalculada", new Double(renta.getRentaFija() + renta.getRentaVariable()));
            parametros.put("origen", renta.getOrigen());
            parametros.put("usuarioAct", renta.getUsuario());

            if (getLogger().isDebugEnabled()) {
                getLogger().debug("[ingresarRentaCalculadaProspecto] parámetros=" + parametros);
            }
            idRegistro = (Integer) conector.ejecutar("prospectos", "ingresoRegistroRTA", parametros);
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("[ingresarRentaCalculadaProspecto] idRegistro=" + idRegistro);
            }
        }
        catch (Exception e) {
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error(
                    "[ingresarRentaCalculadaProspecto] [BCI_FINEX] " + "Error al ingresar renta del prospecto", e);
            }
            throw e;
        }
        if (getLogger().isInfoEnabled()) {
            getLogger().info("[ingresarRentaCalculadaProspecto] [BCI_FINOK] idRegistro=" + idRegistro.intValue());
        }

        return idRegistro.intValue();
    }
    
    /**
     * Método que consulta servicio de previred para obtención de cotizaciones de un rut.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 15/03/2016 Andrés Cea S. (TINet) - Oliver Hidalgo (Ing. Soft. BCI): Versión inicial.</li>
     * <li>1.1 24/05/2016 Rafael Pizarro (TINet) - Claudia López (Ing. Soft. BCI): Se modifica objeto de 
     * respuesta, se agrega logica para el manejo de errores enviados por el servicio, se controla
     * fecha de pago nula.</li>
     * </ul>
     * </p>
     * @param rut rut a consultar.
     * @param dv dv a consultar.
     * @param codigoRegistro identificador de encabezado de las cotizaciones a actualizar en bdd.
     * @return CotizacionPreviredTO objeto de cotizaciones obtenidas desde previred.
     * @throws Exception en caso de error en la obtención desde el servicio.
     * @since 3.3
     */
    public CotizacionPreviredTO obtieneCotizacionesWSPrevired(long rut, char dv, int codigoRegistro)
        throws Exception {
        getLogger().info("[obtieneCotizacionesWSPrevired] [BCI_INI]");
        ConsultarCotizacionesRequest request = new ConsultarCotizacionesRequest();
        String usuarioConexion = TablaValores.getValor(TABLA_PARAMETROS_COTIZACIONES, "AUTENTICAR_PREVIRED",
            "usuario");
        String claveConexion = TablaValores.getValor(TABLA_PARAMETROS_COTIZACIONES, "AUTENTICAR_PREVIRED",
            "clave");
        long rutUsuario = RUTUtil.extraeRUT(usuarioConexion);
        char dvUsuario = RUTUtil.extraeDigitoVerificador(usuarioConexion);

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("[obtieneCotizacionesWSPrevired] [" + rut + "] rut=" + rut);
            getLogger().debug("[obtieneCotizacionesWSPrevired] [" + rut + "] dv=" + rut);
            getLogger().debug("[obtieneCotizacionesWSPrevired] [" + rut + "] codigoRegistro=" + codigoRegistro);
            getLogger().debug("[obtieneCotizacionesWSPrevired] [" + rut + "] rutUsuario=" + rutUsuario);
            getLogger().debug("[obtieneCotizacionesWSPrevired] [" + rut + "] dvUsuario=" + dvUsuario);
        }

        Rut rutAUT = new Rut(rutUsuario, String.valueOf(dvUsuario));
        ParametroAUT aut = new ParametroAUT(rutAUT, claveConexion);
        Rut rutCCA = new Rut(rut, String.valueOf(dv));
        ParametroCCA cca = new ParametroCCA(rutCCA, String.valueOf(codigoRegistro));

        request.setLlave("");
        request.setParametroAUT(aut);
        request.setParametroCCA(cca);

        ConsultarCotizacionesResponse response = null;
        try {
            getLogger().info("[obtieneCotizacionesWSPrevired] consulta sevicio previred.");
            response = crearClienteWSConsulaPrevired().consultarCotizaciones(request);
        }
        catch (Exception e) {
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[obtieneCotizacionesWSPrevired] [BCI_FINEX] [" + rut + "] "
                    + "Error al consultar servicio de cotizaciones previred.", e);
            }
            throw e;
        }

        CotizacionPreviredTO cotizacionPreviredTO = new CotizacionPreviredTO();
        List cotizaciones = new ArrayList();
        RespuestaCCA ccaResponse = response.getRespuestaCCA();
        cotizacionPreviredTO.setCodigo(ccaResponse.getCodigoControl());

        if (ccaResponse.getCodigoControl().equals(CODIGO_CONSULTA_EXITOSA_PREVIRED)){
        getLogger().debug("[obtieneCotizacionesWSPrevired] realiza mapeo desde respuesta ws a CotizacionTO[]");
        DetalleRespuestaCCA[] detalleRespuesta = ccaResponse.getDetalle();
            
        for (int i = 0; i < detalleRespuesta.length; i++) {
            if (getLogger().isDebugEnabled()) {
                    getLogger().debug(
                        "[obtieneCotizacionesWSPrevired] [" + rut + "] detalleRespuesta[" + i + "] = "
                    + detalleRespuesta[i]);
            }
            CotizacionTO cotizacion = new CotizacionTO();
            String mes = String.valueOf(detalleRespuesta[i].getMes());

            mes = StringUtil.rellenaConCeros(mes, LARGO_FORMATO_MMYYYY);

            cotizacion.setMes(FechasUtil.convierteStringADate(mes, new SimpleDateFormat(FORMATO_MMYYYY)));
            cotizacion.setRemuneracionImponible(detalleRespuesta[i].getRemuneracionImponible());
            cotizacion.setMonto(detalleRespuesta[i].getMonto());
                cotizacion.setFechaPago((detalleRespuesta[i].getFechaPago()!=null)?detalleRespuesta[i].getFechaPago().getTime():null);
            cotizacion.setTipoMovimiento(detalleRespuesta[i].getTipoMovimiento());
            cotizacion.setRutEmpleador(detalleRespuesta[i].getRutEmpleador().getCorrelativo());
            cotizacion.setDvEmpleador(detalleRespuesta[i].getRutEmpleador().getDigitoVerificador().charAt(0));
            cotizacion.setAfp(detalleRespuesta[i].getAfp());
            cotizacion.setFechaInformacionLegal(ccaResponse.getLegal().getFechaHora().getTime());

            cotizaciones.add(cotizacion);
        }
            
            
            cotizacionPreviredTO.setCotizaciones((CotizacionTO[]) cotizaciones.toArray(new CotizacionTO[0]));
            cotizacionPreviredTO.setFechaInformacionLegal(ccaResponse.getLegal().getFechaHora().getTime());
            cotizacionPreviredTO.setFirma(ccaResponse.getLegal().getFirma());
            cotizacionPreviredTO.setFolio(ccaResponse.getLegal().getFolio());
        }
            
        if (getLogger().isInfoEnabled()) {
            getLogger().info("[obtieneCotizacionesWSPrevired] [BCI_FINOK] [" + rut + "] "
                + "cantidadCotizaciones = " + cotizaciones.size());
        }

        return cotizacionPreviredTO;
    }
    
    /**
     * Método que instancia implementación de webservice de previred.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 15/03/2016 Andrés Cea S. (TINet) - Oliver Hidalgo (Ing. Soft. BCI): Versión inicial.</li>
     * </ul>
     * </p>
     * @return instancia de servicio previred.
     * @throws IOException en caso de error IO.
     * @throws Exception en caso de error en la obtención desde el servicio.
     * @since 3.1
     */
    private ServicioConsultaRentaCliente_PortType_Stub crearClienteWSConsulaPrevired()
        throws IOException, Exception {
        final String wsdl = TablaValores.getValor(TABLA_PARAMETROS_CALCULORENTA, "servicioConsultaRenta", "wsdl");
        ServicioConsultaRentaCliente_PortType_Stub stub = null;
        if (getLogger().isEnabledFor(Level.INFO)) {
            getLogger().info("[crearClienteWSConsulaPrevired][BCI_INI] wsdl=" + wsdl);
        }

        ServicioConsultaRentaCliente implementacion;
        try {
            getLogger().info("[crearClienteWSConsulaPrevired] instanciando servicio.");
            implementacion = new ServicioConsultaRentaCliente_Impl(wsdl);
            ServicioConsultaRentaCliente_PortType srvPort = implementacion
                .getServicioConsultaRentaCliente_PortType();
            stub = (ServicioConsultaRentaCliente_PortType_Stub) srvPort;
        }
        catch (IOException e) {
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error(
                    "[crearClienteWSConsulaPrevired] [BCI_FINEX] " + "Error en la obtención de cliente ws.", e);
            }
            throw e;
        }
        catch (Exception e) {
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error(
                    "[crearClienteWSConsulaPrevired] [BCI_FINEX] " + "Error en la obtención de cliente ws.", e);
            }
            throw e;
        }
        getLogger().info("[crearClienteWSConsulaPrevired][BCI_FINOK] instancia WS OK.");

        return stub;
    }
    
    /**
     * Método que consulta los pagos asociados a un beneficiario.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 20/03/2016 Ignacio González. (TINet) - Oliver Hidalgo (Ing. Soft. BCI): Versión inicial.</li>
     * <li>1.1 22/04/2016 Ignacio González. (TINet) - Oliver Hidalgo (Ing. Soft. BCI): Se agrega rut empleador a la respuesta.</li>
     * </ul>
     * @param filtro filtro de consulta.
     * @return Listado de los pagos disponibles para el beneficiario.
     * @throws GeneralException en caso de error.
     * @since 3.1
     */
    public PagoTO[] consultaPagosBeneficiario(FiltroConsultaPagoTO filtro) throws GeneralException {
        try {
            if (getLogger().isEnabledFor(Level.INFO)) {
                getLogger().info("[consultaPagosBeneficiario][BCI_INI] Inicio");
            }
            String contexto = TablaValores.getValor("JNDIConfig.parametros", "cluster", "param");
            if (getLogger().isEnabledFor(Level.DEBUG)) {
                getLogger().debug(
                    "[consultaPagosBeneficiario] " + "sp_consulta_pagos_beneficiario contexto: " + contexto);
            }
            ConectorServicioDePersistenciaDeDatos conector = new ConectorServicioDePersistenciaDeDatos(contexto);
            HashMap parametros = new HashMap();

            parametros.put("CHAINED_SUPPORT", "true");
            parametros.put("bnf_rut", filtro.getRutBeneficiario() == 0 ? ""
                : StringUtil.rellenaConCeros((String.valueOf(filtro.getRutBeneficiario()).toString()), LARGO_RUT));
            parametros.put("dcv_tip_cnv_pag", filtro.getTipoPago());
            parametros.put("dop_tip_doc_pag", filtro.getFormaPago());
            parametros.put("odp_fec_pag_desde", filtro.getFechaPagoDesde() == null ? ""
                : FechasUtil.convierteDateAString(filtro.getFechaPagoDesde(), "MM/dd/yyyy"));
            parametros.put("odp_fec_pag_hasta", filtro.getFechaPagoHasta() == null ? ""
                : FechasUtil.convierteDateAString(filtro.getFechaPagoHasta(), "MM/dd/yyyy"));

            List respuesta = null;
            respuesta = conector.consultar("sipeConsulta", "consultaPagosPorBeneficiario", parametros);

            if (respuesta == null) {
                if (getLogger().isEnabledFor(Level.ERROR)) {
                    getLogger().error(
                        "[consultaPagosBeneficiario][GeneralException][BCI_FINOK]: Error respuesta consultaPagosPorBeneficiario es null");
                }
                throw new GeneralException("Error respuesta consultaPagosPorBeneficiario es null");
            }
            PagoTO[] pagosTO = null;

            if (respuesta != null && respuesta.size() > 0) {
                pagosTO = new PagoTO[respuesta.size()];
                int n = 0;
                for (int i = 0; i < respuesta.size(); i++) {
                    pagosTO[n] = new PagoTO();
                    pagosTO[n].setFechaPago(FechasUtil.convierteStringADate(
                        FechasUtil.convierteCalendarAString(
                            ((Calendar) retornarValorDelHashMap(respuesta.get(i), "fechaPago")), "dd/MM/yyyy"),
                        new SimpleDateFormat("dd/MM/yyyy")));
                    pagosTO[n].setNumeroFolio((String) retornarValorDelHashMap(respuesta.get(i), "numeroFolio"));
                    pagosTO[n].setRutBeneficiario(
                        ((Long) retornarValorDelHashMap(respuesta.get(i), "rutBeneficiario")).longValue());
                    pagosTO[n].setRutEmpleador(
                        ((Long) retornarValorDelHashMap(respuesta.get(i), "rutEmpleador")).longValue());
                    pagosTO[n].setMontoAPagar(
                        ((Double) retornarValorDelHashMap(respuesta.get(i), "montoTotal")).doubleValue());
                    pagosTO[n].setGlosaFormaPago((String) retornarValorDelHashMap(respuesta.get(i), "formaPago"));
                    pagosTO[n].setTipoPago((String) retornarValorDelHashMap(respuesta.get(i), "tipoPago"));
                    n++;
                }
            }
            if (pagosTO == null) {
                if (getLogger().isEnabledFor(Level.ERROR)) {
                    getLogger().error(
                        "[consultaPagosBeneficiario][GeneralException][BCI_FINOK]: Error respuesta pagosTO es null");
                }
                throw new GeneralException("Error respuesta pagosTO es null");
            }
            if (getLogger().isEnabledFor(Level.INFO)) {
                getLogger().info("[consultaPagosBeneficiario][BCI_FINOK] pagos :" + pagosTO.toString());
            }
            return pagosTO;
        }
        catch (Exception e) {
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[consultaPagosBeneficiario][BCI_FINEX] Exception: " + e.getMessage(), e);
            }
            throw new GeneralException(ERROR_CONSULTA_PAGOS);
        }
    }
     
     /**
      * Método que retorna valor objeto de acuerdo a objeto de entrada y su atributo .
      * <p>
      * Registro de versiones:
      * <ul>
       * <li>1.0 20/03/2016 Ignacio González. (TINet) - Oliver Hidalgo (Ing. Soft. BCI): Versión inicial.</li>
      * </ul>
      * @param obj objeto de entrada.
      * @param nombre atributo del objeto a obtener.
      * @return Objeto encontrado.
      * @since 3.1
      */
    private Object retornarValorDelHashMap(Object obj, String nombre) {
        Object respuesta = null;
        try{
            respuesta = ((HashMap)obj).get(nombre);
        }
        catch(Exception e){
            if(getLogger().isEnabledFor(Level.ERROR)){
                getLogger().debug("[retornarValorDelHashMap] Exception al rescatar el valor de: " + nombre , e);
            }
        }
        return respuesta;
    }
    
    /**
     * Metodo que registra renta actualizada de acuerdo a reglas de sgc con previred.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 24/05/2016 Rafael Pizarro (TINet) - Claudia López (Ing. Soft. BCI): version inicial.
     * realiza un registro con los valores de las rentas considerando las reglas de sgc con previred.
     * renta fija dividida en miles.
     * </li>
     * </ul>
     * 
     * @param cotizacionCliente renta del cliente.
     * @throws Exception En caso de un error.
     * @return int resultado.
     * @since 3.3
     */
    public int ingresaRentaPreviredDGCRTA(CotizacionClienteVO cotizacionCliente) throws Exception{
        if(getLogger().isDebugEnabled()){
            getLogger().debug("[ingresaRentaPreviredDGCRTA] Inicio [BCI_INI]");
        }
        HashMap parametros = new HashMap();
        parametros.put("CHAINED_SUPPORT", "true");
        Integer resultado = null;
        String contexto = TablaValores.getValor("JNDIConfig.parametros", "cluster", "param");
        if(getLogger().isDebugEnabled()){
            getLogger().debug("[ingresaRentaPreviredDGCRTA] El contexto: " + contexto);
            getLogger().debug("[ingresaRentaPreviredDGCRTA] Llamada al procedimiento sp_ing_usu_rta");
        }
        ConectorServicioDePersistenciaDeDatos conector = new ConectorServicioDePersistenciaDeDatos(contexto);
        if(getLogger().isDebugEnabled()){
            getLogger().debug("[ingresaRentaPreviredDGCRTA] cotizacionCliente [" + cotizacionCliente.toString() + "]");
        }
        try {
            String origenAct = TablaValores
                    .getValor(TABLA_PARAMETROS_CALCULORENTA, "origenAct", "valor");
            String usuarioIngreso = TablaValores
                    .getValor(TABLA_PARAMETROS_CALCULORENTA, "usuarioIngreso", "valor");
            String usuarioActualizacion = TablaValores
                    .getValor(TABLA_PARAMETROS_CALCULORENTA, "usuarioActualizacion", "valor");
            double rentaPreviredFijaRedon = DoubleUtl.redondea(
                    (cotizacionCliente.getMontoRentaFija()/(double)MONTO_MULTI_RENTA),0);
            double rentaPreviredVariableRedon = DoubleUtl.redondea(
                (cotizacionCliente.getMontoRentaVariable()/(double)MONTO_MULTI_RENTA),0);
            double rentaPreviredFija = rentaPreviredFijaRedon*MONTO_MULTI_RENTA;
            double rentaPreviredVariable = rentaPreviredVariableRedon*MONTO_MULTI_RENTA;
            
            String periodoCotiza  = cotizacionCliente.getDetalleCotizacion()[
                                              cotizacionCliente.getDetalleCotizacion().length-1].getPeriodo();
            String dia = periodoCotiza.substring(0, PRIMER_VALOR_SUB);
            String anio = periodoCotiza.substring(PRIMER_VALOR_SUB, SEGUNDO_VALOR_SUB);
            String periodoPrevired = anio+dia;
            parametros.put("rut", String.valueOf(cotizacionCliente.getRutCliente()));
            parametros.put("digitoVerif", String.valueOf(cotizacionCliente.getDvCliente()));
            parametros.put("canal", cotizacionCliente.getCanal());
            parametros.put("origen", origenAct);
            parametros.put("usuarioIngreso",usuarioIngreso);
            parametros.put("usuarioAct", usuarioActualizacion);
            parametros.put("rentaFijaCalculada", new Double(rentaPreviredFija));
            parametros.put("rentaVariableCalculada", new Double(rentaPreviredVariable));
            parametros.put("ultimoPeriodo", periodoPrevired+periodoPrevired);
            resultado = (Integer) conector.ejecutar("dgc", "ingresoRegistroRTA", parametros);
        } 
        catch (ConfiguracionException coExc) {
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[ingresaRentaPreviredDGCRTA] ["
                        + "ConfiguracionException] [BCI_FINEX] " + coExc.getMessage(), coExc);
            }
            throw new Exception("Error al insertar en RTA ["
                    + coExc.getDetalleException() + "]");
        } 
        catch (EjecutarException ejExc) {
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[ingresaRentaPreviredDGCRTA] "
                        + "[EjecutarException] [BCI_FINEX] " + ejExc.getMessage(), ejExc);
            }
            throw new Exception("Error al insertar en RTA  ["
                    + ejExc.getDetalleException() + "]");
        } 
        catch (ServicioDatosException sdExc) {
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[ingresaRentaPreviredDGCRTA] "
                        + "[ServicioDatosException] [BCI_FINEX] " + sdExc.getMessage(), sdExc);
            }
            throw new Exception("Error al insertar en RTA  ["
                    + sdExc.getDetalleException() + "]");
        } 
        catch (Exception e) {
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[ingresaRentaPreviredDGCRTA]"
                        + " [Exception] [BCI_FINEX] " + e.getMessage(), e);
            }
            throw new Exception("Error al insertar en RTA  ["+ e.getMessage()+"]");
        }    
        return resultado.intValue();
    }
    

    /**
    * Método que inserta o actualiza registro de actualización de renta.
    * <p>
    * Registro de versiones:
    * <ul>
    * <li>1.0 01/08/2016, Ariel Araya (SEnTRA) - Sergio Bravo (Ing. Soft. BCI): versión inicial. </li>
    * </ul>
    * <p>
    * 
    * @param registro RegistroActualizaRentaTO con los datos para el ingreso u actualizacion.
    * @throws Exception en caso de error.
    * @since 3.5
    */
   public void ingresarRegistroActualizacionRenta(RegistroActualizaRentaTO registro) throws Exception {
       
       if (getLogger().isEnabledFor(Level.INFO)){
           getLogger().info("[ingresarRegistroActualizacionRenta][BCI_INI] registro[" + registro + "]");
       }
       Integer retorno = null; 
       HashMap parametros = new HashMap();
       String contexto = TablaValores.getValor("JNDIConfig.parametros", "cluster", "param");
       parametros.put("CHAINED_SUPPORT", "true");
       
       parametros.put("act_rut",  new Integer(new Long(registro.getRut()).intValue()));
       parametros.put("act_dv",  String.valueOf(registro.getDv()));
       parametros.put("act_atm",  registro.getIdATM());
       parametros.put("act_id_canal",  registro.getIdCanal());
       parametros.put("act_id_campana",  new Integer(registro.getIdCampana()));
       parametros.put("act_iteracion",  new Integer(registro.getIteracion()));
       parametros.put("act_respuesta",  String.valueOf(registro.getRespuestaCliente()));
       parametros.put("act_cod_error",  registro.getCodError());
       parametros.put("act_glosa_error",  registro.getGlosaError());
      
       
       try{
           ConectorServicioDePersistenciaDeDatos conector = new ConectorServicioDePersistenciaDeDatos(contexto);
           retorno = (Integer)conector.ejecutar("cotizacli","ingresarRegistroActualizacionRenta", parametros);
       }
       catch(Exception e){
           if(getLogger().isEnabledFor(Level.ERROR)){
               getLogger().error("[ingresarRegistroActualizacionRenta][BCI_FINEX][Exception] error: " + e.getMessage(), e);
           }
           throw e;
       }
       if (getLogger().isEnabledFor(Level.INFO)){
           getLogger().info("[ingresarRegistroActualizacionRenta][BCI_FINOK] retorno: " + retorno);
       }
   }
   
}

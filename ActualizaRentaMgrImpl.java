package wcorp.aplicaciones.negocio.ventaymarketing.marketing.atm.proxygenerico.manager;

import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.naming.InitialContext;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import wcorp.aplicaciones.negocio.ventaymarketing.marketing.administraciondecampanasdemarketing.ServiciosAdministracionCampanasDeClientes;
import wcorp.aplicaciones.negocio.ventaymarketing.marketing.administraciondecampanasdemarketing.ServiciosAdministracionCampanasDeClientesHome;
import wcorp.aplicaciones.negocio.ventaymarketing.marketing.atm.to.MensajeriaTO;
import wcorp.bprocess.renta.CalculoRenta;
import wcorp.bprocess.renta.CalculoRentaHome;
import wcorp.bprocess.renta.to.RegistroActualizaRentaTO;
import wcorp.serv.cuentas.InfoCtaCte;
import wcorp.util.EnhancedServiceLocator;
import wcorp.util.StringUtil;
import wcorp.util.TablaValores;
import wcorp.util.atm.CampanaGenericaUtil;
import wcorp.util.com.JNDIConfig;



/**
 * Clase Implementación para la campaña de Actualización de Renta por ATM.
 *
 * <p>
 * Registro de versiones:
 * <ul>
 * <li>1.0 01/08/2016, Pedro Carmona Escobar (SEnTRA) - Sergio Bravo (Ing. Soft. BCI): versión inicial. </li>
 * 
 * </ul>
 * </p>
 *
 * <B>Todos los derechos reservados por Banco de Crédito e Inversiones.</B>
 * <P>
 */
public class ActualizaRentaMgrImpl implements CampanaGenericaMgr {

    /** 
     * Serial de la clase.
     */
    private static final long serialVersionUID = 1L;

    /** 
     * Código de Tx que indica que el cliente verá en pantalla la existencia de la campaña de Actualización de Renta por ATM.
     */
    private static final String TX_INICIAL = "0178+1016";
    
    /** 
     * Código de Tx que indica que el cliente ha aceptado actualizar su renta.
     */
    private static final String TX_ACEPTA_ACTUALIZACION = "0178+0601";
    
    /** 
     * Código de Tx que indica que el cliente ha rechazado actualizar su renta.
     */
    private static final String TX_RECHAZA_ACTUALIZACION = "0178+0602";

    /** 
     * Tabla de parámetros de Actualización de Renta por ATM.
     */
    private static final String TABLA_ACT_RENTA_ATM = "actualizacionRentaPorATM.parametros";
    
    /** 
     * Constante que registra el estado para una campaña aceptada.
     */
    private static final int ESTADO_CAMPANA_ACEPTADA = 1;

    /** 
     * Constante que identifica la tabla de parámetros de transaccions genérica.
     */
    private static final String TABLA_TX_GENERICA = "txGenericaATM.parametros";

    /** 
     * Constante con el código de servicio ATM.
     */
    private static final String CODIGO_SERVICIO_ATM = TablaValores.getValor(TABLA_TX_GENERICA, "datosMarketing", "servicioAsociado")!= null? TablaValores.getValor(TABLA_TX_GENERICA, "datosMarketing", "servicioAsociado") : "011";  

    /**
     * Nombre del campo asociado a la pantalla en la mensajeria de redbanc.
     */
    private static final String CAMPO_TRAMA_REDBANC = "FIELD_OPT44_03";
    
    /**
     * Código del parámetro que permite rescatar el dato de entrada desde la trama entrante.
     */
    private static final String CODIGO_DATO_ENTRADA = "FOP43_TG_DATOS";
    
    /**
     * Código del parámetro que permite rescatar el dato del id del ATM desde la trama entrante.
     */
    private static final String CODIGO_DATO_ID_ATM = "SOURCE_TERM_NBR";
    
    /**
     * Nombre del campo asociado al código de respuesta.
     */
    private static final String CAMPO_CODIGO_RESPUESTA_REDBANC = "ACCT_1_INFO_FLAG";

    /**
     * Posición inicial del telefono de contacto en la mensajeria.
     */
    private static final int PRIMERA_POSICION = 1;

    /**
     * Largo del telefono de contacto en la mensajeria.
     */
    private static final int LARGO_DATO_ENTRADA = 9;
    
    /**
     * Texto que debe ser reemplazado en la trama con el nombre del titular de la tarjeta.
     */
    private static final String CAMPO_NOMBRE = "@NOMBRE@";

    /**
     * Texto que debe ser reemplazado en la trama con el apellido del titular de la tarjeta.
     */
    private static final String CAMPO_APELLIDO = "@APELLIDO@";
    
    /**
     * Llave para acceder a datos de una cola dentro de una tabla de parametros.
     */
    private static final String LLAVE_COLA = "cola";
    
    /**
     * Subllave para acceder al dato de destino de una cola.
     */
    private static final String SUB_LLAVE_DESTINO = "destino";
    
    /**
     * Subllave para acceder al dato de un facotry para una cola.
     */
    private static final String SUB_LLAVE_FACTORY = "factory";
 
    /**
     * Objeto que registra datos durante el flujo de ejecución de la aplicación.
     */
    private transient Logger logger = (Logger)Logger.getLogger(ActualizaRentaMgrImpl.class);



    /**
     * Metodo principal encargado de resolver la transacción.
     *
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 01/08/2016, Pedro Carmona Escobar (SEnTRA) - Sergio Bravo (Ing. Soft. BCI): versión inicial. </li>
     * </ul>
     * </p>
     *
     * @param mensajeria objeto con los datos del la mensajeria enviada por redbanc.
     * @param numeroTarjeta número de la tarjeta de débito.
     * @param datosCliente objeto con los datos de la cuenta del cliente.
     * @return MensajeriaTO con mensajeria de respuesta a redbanc.
     * @throws Exception en caso de algún error.
     * 
     * @since 1.0
     */
    public MensajeriaTO controlaTransaccion(MensajeriaTO mensajeria, String numeroTarjeta, InfoCtaCte datosCliente) throws Exception {

        if (getLogger().isEnabledFor(Level.INFO)){
            getLogger().info( "[controlaTransaccion]["+datosCliente.rut+"][BCI_INI]  mensajeria: " + mensajeria + "   datosCliente: " + datosCliente + "  numeroTarjeta: " + StringUtil.censura(numeroTarjeta));
        }

        Map parametrosRespuesta = new HashMap();
        String codigoRespuesta = "";
        String trama="";

        try {
            if (mensajeria.getCodTransaccion().trim().equals(TX_INICIAL)) {
                if (CampanaGenericaUtil.validarTarjetas(TABLA_ACT_RENTA_ATM, numeroTarjeta)){
                    if (CampanaGenericaUtil.verificaClienteTitular(datosCliente.rut, numeroTarjeta)){
                        Hashtable datosVariables = new Hashtable();
                        datosVariables.put(CAMPO_NOMBRE,datosCliente.nombres.trim());
                        datosVariables.put(CAMPO_APELLIDO,datosCliente.apellidoPaterno.trim());
                        String teclaSi = TablaValores.getValor(TABLA_ACT_RENTA_ATM, mensajeria.getCodTransaccion().trim() + "-teclaSi", "valor");
                        String teclaNo = TablaValores.getValor(TABLA_ACT_RENTA_ATM, mensajeria.getCodTransaccion().trim() + "-teclaNo", "valor");
                        if (getLogger().isEnabledFor(Level.DEBUG)) {
                            getLogger().debug("[controlaTransaccion][" + datosCliente.rut + "] teclaSi: " + teclaSi);
                            getLogger().debug("[controlaTransaccion][" + datosCliente.rut + "] teclaNo: " + teclaNo);
                        }
                        datosVariables.put(teclaSi, String.valueOf(mensajeria.getCodCampana()));
                        datosVariables.put(teclaNo, String.valueOf(mensajeria.getCodCampana()));
                        trama = CampanaGenericaUtil.crearTrama(TABLA_ACT_RENTA_ATM, mensajeria.getCodTransaccion().trim(), datosVariables);
                        if (getLogger().isEnabledFor(Level.DEBUG)) {
                            getLogger().debug("[controlaTransaccion][" + datosCliente.rut + "] trama: " + trama);
                        }
                        parametrosRespuesta.put(CAMPO_TRAMA_REDBANC, trama);
                        codigoRespuesta=TablaValores.getValor(TABLA_ACT_RENTA_ATM, "codigosDeRetorno", "CODIGO_EXITO");
                    } 
                    else {
                        if (getLogger().isEnabledFor(Level.DEBUG)) {
                            getLogger().debug(" Cliente no es titular");
                        }
                        codigoRespuesta=TablaValores.getValor(TABLA_ACT_RENTA_ATM, "codigosDeRetorno", "CODIGO_ERROR_GENERICO"); 
                    }
                } 
                else {
                    if (getLogger().isEnabledFor(Level.DEBUG)) {
                        getLogger().debug(" Aplicacion no activa");
                    }
                    codigoRespuesta=TablaValores.getValor(TABLA_ACT_RENTA_ATM, "codigosDeRetorno", "CODIGO_ERROR_GENERICO"); 
                }                
            }
            
            else if (mensajeria.getCodTransaccion().trim().equals(TX_ACEPTA_ACTUALIZACION)){
                codigoRespuesta=TablaValores.getValor(TABLA_ACT_RENTA_ATM, "codigosDeRetorno", "CODIGO_EXITO");
                Hashtable datosVariables = new Hashtable();
                String canalId = TablaValores.getValor(TABLA_ACT_RENTA_ATM, "canalID", "valor");
                String datoEntrada = (String)mensajeria.getCamposMensajeEntrada().get(CODIGO_DATO_ENTRADA);
                int codigoCampana = Integer.parseInt(datoEntrada.substring(PRIMERA_POSICION, LARGO_DATO_ENTRADA));
                try{
                    RegistroActualizaRentaTO registro = new RegistroActualizaRentaTO();
                    registro.setCodError("");
                    registro.setFechaRegistro(new Date());
                    registro.setRut(datosCliente.rut);
                    registro.setDv(datosCliente.digitoVerificador);
                    registro.setGlosaError("");
                    registro.setIdATM((String)mensajeria.getCamposMensajeEntrada().get(CODIGO_DATO_ID_ATM));
                    registro.setIdCampana(codigoCampana);
                    registro.setIdCanal(canalId);
                    registro.setIteracion(0);
                    registro.setRespuestaCliente('S');
                    CalculoRenta renta = this.obtenerInstanciaEJBCalculoRenta();
                    renta.ingresarRegistroActualizacionRenta(registro);
                    
                    this.publicaJMS(registro);
                    
                    this.actualizarEstadoOferta(codigoCampana, datosCliente.rut, ESTADO_CAMPANA_ACEPTADA, canalId, CODIGO_SERVICIO_ATM);
                }
                catch (Exception e){
                    if (getLogger().isEnabledFor(Level.WARN)){
                        getLogger().warn( "[controlaTransaccion][" + datosCliente.rut + "][Exception] error: " + e.getMessage(),e);
                    }
                }
                trama = CampanaGenericaUtil.crearTrama(TABLA_ACT_RENTA_ATM, mensajeria.getCodTransaccion().trim(), datosVariables);
                if (getLogger().isEnabledFor(Level.DEBUG)) {
                    getLogger().debug("[controlaTransaccion][" + datosCliente.rut + "] trama: " + trama);
                }

                codigoRespuesta=TablaValores.getValor(TABLA_ACT_RENTA_ATM, "codigosDeRetorno", "CODIGO_EXITO");
            }
            
            else if (mensajeria.getCodTransaccion().trim().equals(TX_RECHAZA_ACTUALIZACION)){
                try{
                    String canalId = TablaValores.getValor(TABLA_ACT_RENTA_ATM, "canalID", "valor");
                    String datoEntrada = (String)mensajeria.getCamposMensajeEntrada().get(CODIGO_DATO_ENTRADA);
                    int codigoCampana = Integer.parseInt(datoEntrada.substring(PRIMERA_POSICION, LARGO_DATO_ENTRADA));
                    
                    RegistroActualizaRentaTO registro = new RegistroActualizaRentaTO();
                    registro.setCodError("");
                    registro.setDv(datosCliente.digitoVerificador);
                    registro.setFechaRegistro(new Date());
                    registro.setGlosaError("");
                    registro.setIdATM((String)mensajeria.getCamposMensajeEntrada().get(CODIGO_DATO_ID_ATM));
                    registro.setIdCampana(codigoCampana);
                    registro.setIdCanal(canalId);
                    registro.setIteracion(0);
                    registro.setRespuestaCliente('N');
                    registro.setRut(datosCliente.rut);
                    
                    CalculoRenta renta = this.obtenerInstanciaEJBCalculoRenta();
                    
                    renta.ingresarRegistroActualizacionRenta(registro);
                }
                catch (Exception e){
                    if (getLogger().isEnabledFor(Level.WARN)){
                        getLogger().warn( "[controlaTransaccion][" + datosCliente.rut + "][Exception] error: " + e.getMessage(),e);
                    }
                }
                codigoRespuesta=TablaValores.getValor(TABLA_ACT_RENTA_ATM, "codigosDeRetorno", "CODIGO_EXITO");
                Hashtable datosVariables = new Hashtable();
                trama = CampanaGenericaUtil.crearTrama(TABLA_ACT_RENTA_ATM, mensajeria.getCodTransaccion().trim(), datosVariables);
                if (getLogger().isEnabledFor(Level.DEBUG)) {
                    getLogger().debug("[controlaTransaccion][" + datosCliente.rut + "] trama: " + trama);
                }
            }
            
            else {
                throw new Exception("Tx no existe");             
            }
        }
        catch (Exception ex){
            if (getLogger().isEnabledFor(Level.WARN)){
                getLogger().warn( "[controlaTransaccion][" + datosCliente.rut + "][Exception] error: " + ex.getMessage(),ex);
            }
            trama="";
            codigoRespuesta=TablaValores.getValor(TABLA_ACT_RENTA_ATM, "codigosDeRetorno", "CODIGO_ERROR_GENERICO");
        }
        
        parametrosRespuesta.put(CAMPO_TRAMA_REDBANC, trama);
        parametrosRespuesta.put(CAMPO_CODIGO_RESPUESTA_REDBANC, codigoRespuesta);
        mensajeria.setCamposMensajeRespuesta(parametrosRespuesta);
        mensajeria.setCodigoRespuesta(codigoRespuesta);
        if (getLogger().isEnabledFor(Level.INFO)){ 
            getLogger().info( "[controlaTransaccion][" + datosCliente.rut + "][BCI_FINOK] mensajeria: " + mensajeria );
        }
        return mensajeria;
    }

    

  
    
    /**
     * Método encargado de realizar validación iniciales sobre el cliente que tiene la oferta de Venta de Crédito por ATM.
     *
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 01/08/2016, Pedro Carmona Escobar (SEnTRA) - Sergio Bravo (Ing. Soft. BCI): versión inicial. </li>
     * </ul>
     * </p>
     *
     * @param registro con los datos a publibcar.
     * @return boolean resultado de la publicación.
     * @throws Exception en caso de algún error.
     * 
     * @since 1.0
     */
    private boolean publicaJMS(RegistroActualizaRentaTO registro) throws Exception{
        if (getLogger().isEnabledFor(Level.INFO)){
            getLogger().info("[publicaJMS][BCI_INI] registro: " + registro);
        }
        QueueSession qSession = null;
        QueueSender qsender = null;
        QueueConnection qCon = null;
        boolean retorno = false;
        try{
            InitialContext ic = JNDIConfig.getInitialContext("jms_context");
            String mqueue = TablaValores.getValor(TABLA_ACT_RENTA_ATM, LLAVE_COLA, SUB_LLAVE_DESTINO);
            String mjmsfactory = TablaValores.getValor(TABLA_ACT_RENTA_ATM, LLAVE_COLA, SUB_LLAVE_FACTORY);
            QueueConnectionFactory qConFactory =(QueueConnectionFactory) ic.lookup(mjmsfactory);
            Queue queue = (Queue)ic.lookup(mqueue);
            qCon = qConFactory.createQueueConnection();
            qCon.start();
            qSession = qCon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            qsender = qSession.createSender(queue);
            ObjectMessage qjms = qSession.createObjectMessage();
            qjms.setObject((RegistroActualizaRentaTO) registro);
            qjms.setJMSType("RegistroActualizaRentaTO");
            qsender.send(qjms);
            if (getLogger().isEnabledFor(Level.DEBUG)){
                getLogger().debug("[publicaJMS] menaje enviado.");
            }
            qjms= null;
            retorno = true;
        }
        catch (javax.naming.NamingException ne) {
            if (getLogger().isEnabledFor(Level.WARN)){ 
                getLogger().warn( "[publicaJMS][NamingException] error: " + ne.getMessage(),ne);
            }
        }
        catch (JMSException e) {
            if (getLogger().isEnabledFor(Level.WARN)){ 
                getLogger().warn( "[publicaJMS][JMSException] error: " + e.getMessage(),e);
            }
        }
        catch (Exception ex) {
            if (getLogger().isEnabledFor(Level.WARN)){ 
                getLogger().warn( "[publicaJMS][Exception] error: " + ex.getMessage(),ex);
            }
        }
        finally{
            try{
                if (qsender!=null){
                    qsender.close(); 
                }
                if (qSession!=null){
                    qSession.close(); 
                }
                if (qCon!=null){
                    qCon.close(); 
                }
            }
            catch (Exception e){
                if (getLogger().isEnabledFor(Level.WARN)){ 
                    getLogger().warn( "[publicaJMS][Exception] error: " + e.getMessage(),e);
                }
            }
        }
        if (getLogger().isEnabledFor(Level.INFO)){
            getLogger().info("[publicaJMS][BCI_FINOK] retorno: " + retorno);
        }
        return retorno;
    }

   


    /**
     * Método que permite actualizar el estado de la campaña.
     * <p>
     *
     * Registro de versiones:<UL>
     *
     * <li>1.0 01/08/2016, Pedro Carmona Escobar (SEnTRA) - Sergio Bravo (Ing. Soft. BCI): versión inicial. </li>
     *
     * </ul><p>
     *
     * @param codigoCampana código de la campanas a actualizar .
     * @param rutCliente rut del cliente.
     * @param estadoCampana estado que tendra la campaña (ACEPTADA=1; RECHAZADA=-1).  
     * @param canal canal de la campaña.
     * @param servicio servicio desde el cual se mostrará la campaña.
     *  
     * 
     * @since 1.0
     */
    private void actualizarEstadoOferta(int codigoCampana, long rutCliente, int estadoCampana, String canal, String servicio) {
        if(getLogger().isEnabledFor(Level.INFO)){
            getLogger().info("[actualizarEstadoOferta][BCI_INI] codigoCampana: " + codigoCampana + "  rutCliente: "+ rutCliente + "  estadoCampana: " + estadoCampana + "   canal: " + canal + "   servicio:" + servicio);
        }
        ServiciosAdministracionCampanasDeClientes campanas = null;
        try {
            EnhancedServiceLocator locator = EnhancedServiceLocator.getInstance();
            ServiciosAdministracionCampanasDeClientesHome campanasHome = 
                    (ServiciosAdministracionCampanasDeClientesHome)locator.getHome("wcorp.aplicaciones.negocio."
                            +"ventaymarketing.marketing.administraciondecampanasdemarketing"
                            +".ServiciosAdministracionCampanasDeClientes", 
                            ServiciosAdministracionCampanasDeClientesHome.class);
            campanas = campanasHome.create();
            campanas.actualizarEstadoCampanaDeCliente(codigoCampana, rutCliente, estadoCampana, servicio, canal);
            if(getLogger().isEnabledFor(Level.DEBUG)){
                getLogger().debug("[actualizarEstadoOferta] Campana actualizada" );
            }

        }
        catch ( Exception e ) {
            if (getLogger().isEnabledFor(Level.WARN)) {
                getLogger().warn( "[actualizarEstadoOferta] [Exception] Error: " + e.getMessage(), e);
            }
        }
        if(getLogger().isEnabledFor(Level.INFO)){
            getLogger().info("[actualizarEstadoOferta][BCI_FINOK]");
        }
    }     



    /**
     * <p>Método que crea instancias de EJB CalculoRenta.</p>
     * 
     * Registro de versiones:<ul>
     * <li>1.0 01/08/2016, Pedro Carmona Escobar (SEnTRA) - Sergio Bravo (Ing. Soft. BCI): versión inicial. </li>
     * </ul>
     * 
     * @return CalculoRenta objeto con la instancia del EJB.
     * @throws Exception en caso de error.
     * @since 1.0
     */
    private CalculoRenta obtenerInstanciaEJBCalculoRenta() throws Exception {
        if(getLogger().isEnabledFor(Level.INFO)){
            getLogger().info("[obtenerInstanciaEJBCalculoRenta][BCI_INI] ");
        }
        CalculoRenta renta = ((CalculoRentaHome) EnhancedServiceLocator.getInstance()
                .getHome("wcorp.bprocess.renta.CalculoRenta", CalculoRentaHome.class)).create();

        if(getLogger().isEnabledFor(Level.INFO)){
            getLogger().info("[obtenerInstanciaEJBCalculoRenta][BCI_FINOK]");
        }
        return renta;
    }

    /**
     * <p>Método que retorna la instancia de Logger para esta clase.</p>
     * 
     * Registro de versiones:
     * <ul>
     * <li>1.0 01/08/2016, Pedro Carmona Escobar (SEnTRA) - Sergio Bravo (Ing. Soft. BCI): versión inicial. </li>
     * </ul>
     * @return log para la clase.
     * 
     * @since 1.0
     */
    private Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger(this.getClass());
        }
        return logger;
    }

} 

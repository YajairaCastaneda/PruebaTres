package wcorp.jms.atm.renta;


import javax.ejb.MessageDrivenBean;
import javax.ejb.MessageDrivenContext;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import wcorp.bprocess.renta.CalculoRenta;
import wcorp.bprocess.renta.CalculoRentaHome;
import wcorp.bprocess.renta.to.RegistroActualizaRentaTO;
import wcorp.util.EnhancedServiceLocator;
import wcorp.util.TablaValores;

/**
 * Clase MDB que permite realizar acci�n sobre los mensaje de la cola Actualizaci�n de Renta ATM.
 *
 * <p>
 * Registro de versiones:
 * <ul>
 * <li>1.0 01/08/2016, Pedro Carmona Escobar (SEnTRA) - Sergio Bravo (Ing. Soft. BCI): versi�n inicial. </li>
 * 
 * </ul>
 * </p>
 *
 * <B>Todos los derechos reservados por Banco de Cr�dito e Inversiones.</B>
 * <P>
 */
public class ReintentoBean implements MessageDrivenBean, MessageListener {

    /** 
     * Serial de la clase.
     */
    private static final long serialVersionUID = 1L;
    
    /** 
     * Tabla de par�metros de Actualizaci�n de Renta por ATM.
     */
    private static final String TABLA_ACT_RENTA_ATM = "actualizacionRentaPorATM.parametros";
    
    /** 
     * Llave para acceder al codigo de servicio.
     */
    private static final String LLAVE_SERVICIO = "codServicio";
    
    /** 
     * Sub llave para acceder al valor del c�digo de servicio.
     */
    private static final String SUB_LLAVE = "valor";
    
    /** 
     * Tabla de par�metros de C�lculo de Renta.
     */
    private static final String TABLA_CAL_RENTA = "CalculoRenta.parametros";
    
    /** 
     * Sub llave para acceder a la descripci�n de un c�digo retornado.
     */
    private static final String SUB_LLAVE_DESC = "Desc";
    
    /** 
     * Sub llave para acceder al c�digo retornado.
     */
    private static final String SUB_LLAVE_CODIGO = "cod";
    
    /** 
     * CLlave para acceder al codigo de error.
     */
    private static final String LLAVE_ERROR = "errorGeneral";

    /**
     * Objeto que registra datos durante el flujo de ejecuci�n de la aplicaci�n.
     */
    private transient Logger logger = (Logger)Logger.getLogger(ReintentoBean.class);

    /**
     * Atributo con el contexto para el MDB.
     */
    private MessageDrivenContext messageContext = null;

    public void ejbCreate(){
    }


    /**
     * M�todo que se ejecuta para remover la instancia del MDB.
     *
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 01/08/2016, Pedro Carmona Escobar (SEnTRA) - Sergio Bravo (Ing. Soft. BCI): versi�n inicial. </li>
     * </ul>
     * </p>
     * 
     * @since 1.0
     */
    public void ejbRemove(){
        messageContext = null;
    }

    public void setMessageDrivenContext(MessageDrivenContext context){
        this.messageContext = context;
    }

    /**
     * Metodo principal encargado de resolver la transacci�n.
     *
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 01/08/2016, Pedro Carmona Escobar (SEnTRA) - Sergio Bravo (Ing. Soft. BCI): versi�n inicial. </li>
     * </ul>
     * </p>
     *
     * @param message objeto con la informaci�n que se ha traido desde la cola.
     * 
     * @since 1.0
     */
    public void onMessage(Message message){
        if (getLogger().isEnabledFor(Level.INFO)){
            getLogger().info( "[onMessage][BCI_INI]  message: " + message );
        }
        if(message instanceof ObjectMessage){
            ObjectMessage msg = (ObjectMessage) message;
            try{
                Object dto = msg.getObject();
                if(getLogger().isEnabledFor(Level.DEBUG)){
                    getLogger().debug("[onMessage] dto instanceof RegistroActualizaRentaTO: "+(dto instanceof RegistroActualizaRentaTO));
                }
                if(dto instanceof RegistroActualizaRentaTO){
                    RegistroActualizaRentaTO reg = (RegistroActualizaRentaTO) dto;
                    if(getLogger().isEnabledFor(Level.DEBUG)){
                        getLogger().debug("[onMessage] reg: "+reg);
                    }
                    String resultado = "";
                    String descError = "";
                    CalculoRenta renta = this.obtenerInstanciaEJBCalculoRenta();
                    String codServicio = TablaValores.getValor(TABLA_ACT_RENTA_ATM, LLAVE_SERVICIO, SUB_LLAVE);

                    try{
                        resultado = renta.actualizarRentaClienteCotizaciones(reg.getRut(), reg.getDv(), reg.getIdCanal(), codServicio);
                        if(getLogger().isEnabledFor(Level.DEBUG)){
                            getLogger().debug("[onMessage] resultado: "+resultado);
                        }
                        descError = TablaValores.getValor(TABLA_CAL_RENTA, resultado.trim(), SUB_LLAVE_DESC);   
                    }
                    catch (Exception e){
                        if (getLogger().isEnabledFor(Level.WARN)) {
                            getLogger().warn( "[onMessage] [Exception] Error: " + e.getMessage(), e);
                        }
                        resultado = TablaValores.getValor(TABLA_ACT_RENTA_ATM, LLAVE_ERROR, SUB_LLAVE_CODIGO);
                        descError = TablaValores.getValor(TABLA_ACT_RENTA_ATM, LLAVE_ERROR, SUB_LLAVE_DESC);                        
                    }
                    reg.setCodError(resultado);
                    reg.setGlosaError(descError);
                    reg.setIteracion(reg.getIteracion() + 1);
                    if(getLogger().isEnabledFor(Level.DEBUG)){
                        getLogger().debug("[onMessage] registro a actualizar: "+reg);
                    }
                    renta.ingresarRegistroActualizacionRenta(reg);
                }
            }
            catch(Exception e){
                if (getLogger().isEnabledFor(Level.WARN)) {
                    getLogger().warn( "[onMessage] [Exception] Error GENERAL: " + e.getMessage(), e);
                }
                messageContext.setRollbackOnly();
            }
            if (getLogger().isEnabledFor(Level.INFO)){
                getLogger().info( "[onMessage][BCI_FINOK]");
            }            
        }

    }
    
    /**
     * <p>M�todo que crea instancias de EJB CalculoRenta.</p>
     * 
     * Registro de versiones:<ul>
     * <li>1.0 01/08/2016, Pedro Carmona Escobar (SEnTRA) - Sergio Bravo (Ing. Soft. BCI): versi�n inicial. </li>
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
     * <p>M�todo que retorna la instancia de Logger para esta clase.</p>
     * 
     * Registro de versiones:
     * <ul>
     * <li>1.0 01/08/2016, Pedro Carmona Escobar (SEnTRA) - Sergio Bravo (Ing. Soft. BCI): versi�n inicial. </li>
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

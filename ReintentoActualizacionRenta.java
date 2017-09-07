package cl.bci.aplicaciones.renta;

import java.io.Serializable;
import java.util.Properties;

import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import wcorp.bprocess.renta.to.RegistroActualizaRentaTO;

/**
 * Clase que invoca  la logica  para hacer el proceso de carga.
 * 
 * <p>
 * Registro de versiones:
 * <ul>
 * <li>1.0 01/08/2016 Manuel Sepulveda (SEnTRA) - Sergio Bravo (Ing. Soft. BCI): versión inicial.</li>
 * </ul>
 * </p>
 * <B>Todos los derechos reservados por Banco de Crédito e Inversiones.</B>
 */
public class ReintentoActualizacionRenta {

    /**
     * logger de la clase.
     */
    private static Logger logger = Logger.getLogger(ReintentoActualizacionRenta.class);

    /**
     * numero minimo de datos entrada.
     */
    private static final int CANTIDAD_MINIMA_PARAMETROS_ENTRADA = 6;
    
    /**
     * indica la posicion en el arreglo para el valor del factory para la cola jms.
     */
    private static final int POSICION_FACTORY_JMS = 0;
    
    /**
     * indica la posicion en el arreglo para el valor del queue para la cola jms.
     */
    private static final int POSICION_QUEUE_JMS = 1;
    
    /**
     * posicion en el arreglo para el servidor de la cola jms.
     */
    private static final int POSICION_SERVIDOR_JMS = 2;
    
    /**
     * posicion en el arreglo que indica la clase a usar para la cola jms.
     */
    private static final int POSICION_FACTORY_CLASS = 3;
    
    /**
     * posicion del rut del cliente.
     */
    private static final int POSICION_RUT_CLIENTE = 4;
    
    /**
     * posicion del dv del cliente.
     */
    private static final int POSICION_DV_CLIENTE = 5;
    
    /**
     * posicion de la iteracion actual de reintento de actualizacion de renta.
     */
    private static final int POSICION_ITERACION_ACTUAL = 6;
    
    /**
     * Codigo de error a devolver para ser utilizado en la shell que invoco este proceso. 
     */
    private static final int CODIGO_ESTADO_ERROR = 4;
    
    /**
     * Codigo de estado OK a devolver para ser utilizado en la shell que invoco este proceso. 
     */
    private static final int CODIGO_ESTADO_OK = 0;
    
    
    /**
     * Metodo main que ejecuta el proceso de reintento de actualizacion de renta por cola JMS.
     * 
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 01/08/2016 Manuel Sepulveda (SEnTRA) - Sergio Bravo (Ing. Soft. BCI): versión inicial.</li>
     * </ul>
     * </p>
     * 
     * @param args argumentos que vienen de entrada para el proceso.
     * @since 1.0
     */ 
    public static void main(String[] args) {
        if (logger.isEnabledFor(Level.INFO)) {
            logger.info("[main] [BCI_INI] args: " + args);
        }
        try {
            int largo = args.length;
            if (logger.isEnabledFor(Level.DEBUG)) {
                logger.debug("[main] Cantidad de argumentos: " + largo);
            }
            if (largo > CANTIDAD_MINIMA_PARAMETROS_ENTRADA) {
                String jmsfactory = (args[POSICION_FACTORY_JMS]);
                if (jmsfactory.equals("")) {
                    if (logger.isEnabledFor(Level.ERROR)) {
                        logger.error("[main][BCI_FINEX] No se ingreso el factory.");
                    }
                    System.out.println(CODIGO_ESTADO_ERROR);
                    return;
                }
                String queueJms = (args[POSICION_QUEUE_JMS]);
                if (queueJms.equals("")) {
                    if (logger.isEnabledFor(Level.ERROR)) {
                        logger.error("[main] [BCI_FINEX] No se ingreso el queue.");
                    }
                    System.out.println(CODIGO_ESTADO_ERROR);
                    return;
                }
                String servidorJms = (args[POSICION_SERVIDOR_JMS]);
                if (servidorJms.equals("")) {
                    if (logger.isEnabledFor(Level.ERROR)) {
                        logger.error("[main] [BCI_FINEX] No se ingreso el servidor.");
                    }
                    System.out.println(CODIGO_ESTADO_ERROR);
                    return;
                }
                String factoryClass = (args[POSICION_FACTORY_CLASS]);
                if (factoryClass.equals("")) {
                    if (logger.isEnabledFor(Level.ERROR)) {
                        logger.error("[main] [BCI_FINEX] No se ingreso el factory Class.");
                    }
                    System.out.println(CODIGO_ESTADO_ERROR);
                    return;
                }
                RegistroActualizaRentaTO registroActualizaRenta = new RegistroActualizaRentaTO();
                registroActualizaRenta.setRut(Long.parseLong(args[POSICION_RUT_CLIENTE]));
                registroActualizaRenta.setDv(args[POSICION_DV_CLIENTE].charAt(POSICION_FACTORY_JMS));
                registroActualizaRenta.setIteracion(Integer.parseInt(args[POSICION_ITERACION_ACTUAL]));
                if (logger.isEnabledFor(Level.DEBUG)) {
                    logger.debug("[main] registroActualizaRenta: " + registroActualizaRenta);
                }
                enviarMensajeJMS(jmsfactory, queueJms, servidorJms, factoryClass, registroActualizaRenta);
            }
            else {
                if (logger.isEnabledFor(Level.ERROR)) {
                    logger.error("[main] [BCI_FINEX] No tiene el minimo de parametros para iniciar la ejecucion.");
                }                
                System.out.println(CODIGO_ESTADO_ERROR);
                return;
            }
        }
        catch (Exception e) {
            if (logger.isEnabledFor(Level.ERROR)) {
                logger.error("[main] [BCI_FINEX] [Exception] " + e.getMessage(), e);
            } 
            System.out.println(CODIGO_ESTADO_ERROR);
            return;
        }
        if (logger.isEnabledFor(Level.INFO)) {
            logger.info("[main] [BCI_FINOK]");
        } 
        System.out.println(CODIGO_ESTADO_OK);
    }
     
    /**
     * Metodo encargado de crear coneccion con la cola JMS indicada y enviar el objeto para el proceso de reintento
     * de actualizacio de renta.
     * 
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 01/08/2016 Manuel Sepulveda (SEnTRA) - Sergio Bravo (Ing. Soft. BCI): versión inicial.</li>
     * </ul>
     * </p>
     * 
     * @param jmsfactory se carga el valor del factory.
     * @param aQueue se carga el valor que tiene la queue.
     * @param servidor guarda la ip del servidor de conexion.
     * @param factoryClass guarda los datos del JNDI de conexion.
     * @param object trae los parametros de entrada.
     * @throws Exception canaliza las excepciones.
     * @since 1.0
     */
    private static void enviarMensajeJMS(String jmsfactory, String aQueue, String servidor, String factoryClass, 
            Serializable object) throws Exception{
        if (logger.isEnabledFor(Level.INFO)) {
            logger.info("[enviarMensajeJMS] [BCI_INI] mjmsfactory [" + jmsfactory + "] queue [" + aQueue + "] servidor [" + servidor + "] factoryClass [" + factoryClass + "]");
        } 
        try{
            Properties p = new Properties();
            p.put(Context.INITIAL_CONTEXT_FACTORY, factoryClass);
            p.put(Context.PROVIDER_URL, servidor);
            InitialContext ic = new InitialContext(p);
            if (logger.isEnabledFor(Level.DEBUG)) {
                logger.debug("[enviarMensajeJMS] Se obtuvo Initial Context");
            }
            QueueConnectionFactory qConFactory =(QueueConnectionFactory) ic.lookup(jmsfactory);
            if (logger.isEnabledFor(Level.DEBUG)) {
                logger.debug("[enviarMensajeJMS] Se obtuvo Factory");
            }
            Queue queue = (Queue)ic.lookup(aQueue);
            if (logger.isEnabledFor(Level.DEBUG)) {
                logger.debug("[enviarMensajeJMS] Se obtuvo Queue");
            }
            QueueConnection qCon = qConFactory.createQueueConnection();
            qCon.start();
            QueueSession qSession = qCon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            QueueSender qsender = qSession.createSender(queue);
            ObjectMessage objetoMensaje = qSession.createObjectMessage();
            objetoMensaje.setObject(object);
            qsender.send(objetoMensaje);
            if (logger.isEnabledFor(Level.DEBUG)) {
                logger.debug("[enviarMensajeJMS] Objeto enviado");
            }
            objetoMensaje= null;
            qsender.close();
            qSession.close();
            qCon.close();
        }
        catch (Exception e) {
            if (logger.isEnabledFor(Level.ERROR)) {
                logger.error("[enviarMensajeJMS] [BCI_FINEX] [Exception] " + e.getMessage(), e);
            } 
            throw e;
        }
        if (logger.isEnabledFor(Level.INFO)) {
            logger.info("[enviarMensajeJMS] [BCI_FINOK]");
        }
    }
}

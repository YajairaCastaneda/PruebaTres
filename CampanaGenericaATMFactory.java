package wcorp.aplicaciones.negocio.ventaymarketing.marketing.atm.proxygenerico.factory;

import wcorp.aplicaciones.negocio.ventaymarketing.marketing.atm.proxygenerico.manager.ActualizaRentaMgrImpl;
import wcorp.aplicaciones.negocio.ventaymarketing.marketing.atm.proxygenerico.manager.ActualizacionDeDatosMgrImpl;
import wcorp.aplicaciones.negocio.ventaymarketing.marketing.atm.proxygenerico.manager.AnticipoDeSueldoMgrImpl;
import wcorp.aplicaciones.negocio.ventaymarketing.marketing.atm.proxygenerico.manager.CampanaGenericaMgr;
import wcorp.aplicaciones.negocio.ventaymarketing.marketing.atm.proxygenerico.manager.DonacionMgrImpl;
import wcorp.aplicaciones.negocio.ventaymarketing.marketing.atm.proxygenerico.manager.VentaCreditoConsumoMgrImpl;
import wcorp.aplicaciones.negocio.ventaymarketing.marketing.atm.proxygenerico.manager.VentaGenericaMgrImpl;
import wcorp.aplicaciones.negocio.ventaymarketing.marketing.atm.proxygenerico.manager.VentaMultiproteccionMgrImpl;

/**
 * Clase responsable de crear una instancia de un adaptador para el Servicio
 * Generico de campañas por ATM.
 * 
 * <p>
 * Registro de versiones:
 * <ul>
 * <li>1.0 14/12/2011, Andrés Morán Ortiz (SEnTRA): versión inicial.
 * <li>1.1 14/09/2012, Andrés Morán Ortiz (SEnTRA): Se agrega código de instancia para 
 * campaña de Venta Genérica. 
 * 
 * <li>1.2 26/06/2015, Pedro Carmona Escobar (SEnTRA) - Renato Oportus (Ing. Soft. BCI): Se agrega constante 'VENTA_CREDITO_CONSUMO' y 
 *                          se modifica el método {@link #getInstance(int)}. 
 * 
 * <li>1.3 01/08/2016, Pedro Carmona Escobar (SEnTRA) - Sergio Bravo (Ing. Soft. BCI): Se agrega constante 'ACT_RENTA' y 
 *                          se modifica el método {@link #getInstance(int)}. 
 * </ul>
 * </p>
 * 
 * <B>Todos los derechos reservados por Banco de Crédito e Inversiones.</B>
 * <P>
 */
public abstract class CampanaGenericaATMFactory {

    /** Código de Tx inicial (ciega) de campañas genericas. */
    public static final String TABLA_TX_GENERICA = "txGenericaATM.parametros";
    
    /** Código de la campaña de actualización de datos. */
    private static final int ACTUALIZACION_DATOS = 1;
    
    /** Código de la campaña de venta de seguros multiprotección. */
    private static final int VENTA_MULTIPROTECCION = 2;
    
    /** Código de la campaña de donaciones. */    
    private static final int DONACIONES = 3;
    
    /** Código de la campaña de anticipo de sueldos. */    
    private static final int ANTICIPO_SUELDO = 4;

    /** Código de la campaña de Venta Genérica. */    
    private static final int VENTA_GENERICA = 5;
    
    /**
     * Código de la campaña de Venta Crédito Consumo.
     */    
    private static final int VENTA_CREDITO_CONSUMO = 6;
    
    /**
     * Código de la campaña de Actualización de Renta.
     */    
    private static final int ACT_RENTA = 7;

    /**
     * Metodo responsable de construir una instancia del Adaptador Generico para
     * la campaña de acuerdo al código asociado
     * 
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 14/12/2011, Andrés Morán Ortiz (SEnTRA)- versión inicial.
     * <li>1.1 14/09/2012, Andrés Morán Ortiz (SEnTRA)- Se agrega campaña de Venta Genérica.
     * 
     * <li>1.2 26/06/2015, Pedro Carmona Escobar (SEnTRA) - Renato Oportus (Ing. Soft. BCI): Se agrega código para la campaña Venta Crédito Consumo. 
     * 
     * <li>1.3 01/08/2016, Pedro Carmona Escobar (SEnTRA) - Sergio Bravo (Ing. Soft. BCI): Se agrega código para la campaña Actualización de Renta.
     *  
     * </ul>
     * </p>
     * 
     * @param codigo de la campaña a resolver.
     * 
     * @return retorna una Instancia de la clase que resolvera la transacción.
     * 
     * @throws IllegalArgumentException Excepción de argumento ilegal.
     * 
     * @since 1.0
     */
    public static CampanaGenericaMgr getInstance(int codigo) throws IllegalArgumentException {
        switch (codigo) {
        case ACTUALIZACION_DATOS:
            return new ActualizacionDeDatosMgrImpl();
        case VENTA_MULTIPROTECCION:
            return new VentaMultiproteccionMgrImpl();
        case DONACIONES:
            return new DonacionMgrImpl();
        case ANTICIPO_SUELDO:
            return new AnticipoDeSueldoMgrImpl();
        case VENTA_GENERICA:
            return new VentaGenericaMgrImpl();
        case VENTA_CREDITO_CONSUMO:
            return new VentaCreditoConsumoMgrImpl();
        case ACT_RENTA:
            return new ActualizaRentaMgrImpl();            
        default:
            throw new IllegalArgumentException("El código " + codigo + " no está soportado");
        }
    }
}

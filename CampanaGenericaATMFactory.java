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
 * Generico de campa�as por ATM.
 * 
 * <p>
 * Registro de versiones:
 * <ul>
 * <li>1.0 14/12/2011, Andr�s Mor�n Ortiz (SEnTRA): versi�n inicial.
 * <li>1.1 14/09/2012, Andr�s Mor�n Ortiz (SEnTRA): Se agrega c�digo de instancia para 
 * campa�a de Venta Gen�rica. 
 * 
 * <li>1.2 26/06/2015, Pedro Carmona Escobar (SEnTRA) - Renato Oportus (Ing. Soft. BCI): Se agrega constante 'VENTA_CREDITO_CONSUMO' y 
 *                          se modifica el m�todo {@link #getInstance(int)}. 
 * 
 * <li>1.3 01/08/2016, Pedro Carmona Escobar (SEnTRA) - Sergio Bravo (Ing. Soft. BCI): Se agrega constante 'ACT_RENTA' y 
 *                          se modifica el m�todo {@link #getInstance(int)}. 
 * </ul>
 * </p>
 * 
 * <B>Todos los derechos reservados por Banco de Cr�dito e Inversiones.</B>
 * <P>
 */
public abstract class CampanaGenericaATMFactory {

    /** C�digo de Tx inicial (ciega) de campa�as genericas. */
    public static final String TABLA_TX_GENERICA = "txGenericaATM.parametros";
    
    /** C�digo de la campa�a de actualizaci�n de datos. */
    private static final int ACTUALIZACION_DATOS = 1;
    
    /** C�digo de la campa�a de venta de seguros multiprotecci�n. */
    private static final int VENTA_MULTIPROTECCION = 2;
    
    /** C�digo de la campa�a de donaciones. */    
    private static final int DONACIONES = 3;
    
    /** C�digo de la campa�a de anticipo de sueldos. */    
    private static final int ANTICIPO_SUELDO = 4;

    /** C�digo de la campa�a de Venta Gen�rica. */    
    private static final int VENTA_GENERICA = 5;
    
    /**
     * C�digo de la campa�a de Venta Cr�dito Consumo.
     */    
    private static final int VENTA_CREDITO_CONSUMO = 6;
    
    /**
     * C�digo de la campa�a de Actualizaci�n de Renta.
     */    
    private static final int ACT_RENTA = 7;

    /**
     * Metodo responsable de construir una instancia del Adaptador Generico para
     * la campa�a de acuerdo al c�digo asociado
     * 
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 14/12/2011, Andr�s Mor�n Ortiz (SEnTRA)- versi�n inicial.
     * <li>1.1 14/09/2012, Andr�s Mor�n Ortiz (SEnTRA)- Se agrega campa�a de Venta Gen�rica.
     * 
     * <li>1.2 26/06/2015, Pedro Carmona Escobar (SEnTRA) - Renato Oportus (Ing. Soft. BCI): Se agrega c�digo para la campa�a Venta Cr�dito Consumo. 
     * 
     * <li>1.3 01/08/2016, Pedro Carmona Escobar (SEnTRA) - Sergio Bravo (Ing. Soft. BCI): Se agrega c�digo para la campa�a Actualizaci�n de Renta.
     *  
     * </ul>
     * </p>
     * 
     * @param codigo de la campa�a a resolver.
     * 
     * @return retorna una Instancia de la clase que resolvera la transacci�n.
     * 
     * @throws IllegalArgumentException Excepci�n de argumento ilegal.
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
            throw new IllegalArgumentException("El c�digo " + codigo + " no est� soportado");
        }
    }
}

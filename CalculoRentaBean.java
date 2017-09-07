package wcorp.bprocess.renta;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.ejb.CreateException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import wcorp.aplicaciones.clientes.informaciondecliente.antecedentesclientes.implementation.AntecedentesClientes;
import wcorp.aplicaciones.clientes.informaciondecliente.antecedentesclientes.implementation.AntecedentesClientesHome;
import wcorp.aplicaciones.clientes.informaciondecliente.antecedentesclientes.to.ResultConsultaAntecedentesEconomicosPersonasTO;
import wcorp.aplicaciones.productos.colocaciones.solicitudes.instantaneas.EvaluadorReglasDeRenta;
import wcorp.aplicaciones.productos.colocaciones.solicitudes.instantaneas.dao.webservice.AntecedentesPrevisionalesDAO;
import wcorp.aplicaciones.productos.colocaciones.solicitudes.instantaneas.to.DatosBiometricosTO;
import wcorp.aplicaciones.productos.colocaciones.solicitudes.instantaneas.to.IdentificadorReglaRentaTO;
import wcorp.aplicaciones.productos.colocaciones.solicitudes.instantaneas.to.ReglaRentaTO;
import wcorp.aplicaciones.productos.colocaciones.solicitudes.instantaneas.to.RentaSolicitanteTO;
import wcorp.aplicaciones.productos.colocaciones.solicitudes.instantaneas.util.ReglaRentaUtil;
import wcorp.aplicaciones.productos.colocaciones.solicitudes.instantaneas.util.SolicitudInstantaneaUtil;
import wcorp.bprocess.cliente.Cliente;
import wcorp.bprocess.cliente.ClienteHome;
import wcorp.bprocess.cuentas.CuentasDelegate;
import wcorp.bprocess.renta.dao.CalculoRentaDAO;
import wcorp.bprocess.renta.reglas.estrategias.CalculoRentaClienteStrategy;
import wcorp.bprocess.renta.reglas.estrategias.CalculoRentaProspectoStrategy;
import wcorp.bprocess.renta.reglas.estrategias.CalculoRentaProspectoTrfStrategy;
import wcorp.bprocess.renta.reglas.evaluador.EvaluadorActualizacionRenta;
import wcorp.bprocess.renta.reglas.to.CotizacionPreviredTO;
import wcorp.bprocess.renta.reglas.to.CotizacionTO;
import wcorp.bprocess.renta.reglas.to.FiltroConsultaPagoTO;
import wcorp.bprocess.renta.reglas.to.PagoTO;
import wcorp.bprocess.renta.reglas.to.RentaTO;
import wcorp.bprocess.renta.reglas.util.ReglasActualizacionRentaUtil;
import wcorp.bprocess.renta.to.EncabezadoCalculoAutomaticoTO;
import wcorp.bprocess.renta.to.RegistroActualizaRentaTO;
import wcorp.bprocess.renta.to.ResultadoRegistroRentaTO;
import wcorp.bprocess.renta.vo.CotizacionClienteVO;
import wcorp.bprocess.renta.vo.DetalleCotizacionClienteVO;
import wcorp.bprocess.renta.vo.ErrorCotizacionVO;
import wcorp.bprocess.simulacion.SvcMetodosForaneos;
import wcorp.bprocess.simulacion.SvcMetodosForaneosImpl;
import wcorp.env.util.WCorpUtils;
import wcorp.model.actores.cliente.ClientePersona;
import wcorp.serv.clientes.ClientesException;
import wcorp.serv.clientes.DatosPersona;
import wcorp.serv.clientes.RetornoTipCli;
import wcorp.serv.clientes.ServiciosCliente;
import wcorp.serv.clientes.ServiciosClienteHome;
import wcorp.serv.cuentas.ListaCuentas;
import wcorp.serv.economia.InfoFinanciera;
import wcorp.serv.economia.ServiciosEconomia;
import wcorp.serv.economia.ServiciosEconomiaHome;
import wcorp.serv.misc.ServiciosMiscelaneos;
import wcorp.serv.misc.ServiciosMiscelaneosHome;
import wcorp.serv.renta.BoletaHonorarios;
import wcorp.serv.renta.CalculoHistorico;
import wcorp.serv.renta.CalculoRentaException;
import wcorp.serv.renta.Constantes;
import wcorp.serv.renta.Dai;
import wcorp.serv.renta.DetalleItem;
import wcorp.serv.renta.EstadoCalculo;
import wcorp.serv.renta.LiquidacionDeSueldo;
import wcorp.serv.renta.Parametros;
import wcorp.serv.renta.RentaCliente;
import wcorp.serv.renta.ServicioCalculoRenta;
import wcorp.serv.renta.ServicioCalculoRentaHome;
import wcorp.serv.renta.dao.RentaTcorpDAO;
import wcorp.serv.renta.to.CalculoRentaTO;
import wcorp.serv.renta.to.DetalleArriendoTO;
import wcorp.util.DoubleUtl;
import wcorp.util.EnhancedServiceLocator;
import wcorp.util.ErroresUtil;
import wcorp.util.FechasUtil;
import wcorp.util.GeneralException;
import wcorp.util.NumerosUtil;
import wcorp.util.RUTUtil;
import wcorp.util.StringUtil;
import wcorp.util.TablaValores;
import wcorp.util.bee.MultiEnvironment;
import wcorp.util.com.JNDIConfig;
import wcorp.util.com.TuxedoException;
import wcorp.util.renta.PreviredUtil;

import ws.serviciob2b.previred.obtenerCotizaciones.CWSMensajeriaXML;
import ws.serviciob2b.previred.obtenerCotizaciones.CWSMensajeriaXMLService;
import ws.serviciob2b.previred.obtenerCotizaciones.CWSMensajeriaXMLService_Impl;
import ws.serviciob2b.previred.obtenerCotizaciones.CWSMensajeriaXML_Stub;

/**
 * <b>EJB DE  CÁLCULO DE LA RENTA</b>
 * <p>
 * Este Bean permite interactuar con la información de la renta del cliente por
 * medio de la clase ServicioCalculoRentaBean
 * <p>
 *
 * Registro de versiones:<ul>
 *
 * <li> 1.0  Sin informacion :  Version Inicial</li>
 *
 * <li> 2.0  06/12/2005  Emilio Fuentealba Silva(SEnTRA)    : se agregan los métodos cargaIngresosAdicionales ,setIngresosAdicionales
 * y setRentaFija los cuales permite el almacenamiento y recuperación de los ingresos adicionales del cliente, para
 * arriendo de bienes raíces, pesiones o jubilaciones y pensión alimenticia.</li>
 *
 * <li> 2.0  06/12/2005  Emilio Fuentealba Silva (SEnTRA) : modificación de los métodos setDatosCliente que guarda el canal
 * por donde se realizan los cambios y obtieneCalculoHistoricoCliente que obtiene los datos historicos del cliente, incluyendo a los ingresos adicionales.</li>
 *
 * <li> 3.0  06/04/2006  Nelly Reyes Quezada (SEnTRA) : Se agrega log para mayor información en detección de errores.</li>
 *
 * <li> 3.1  15/05/2006  Nelly Reyes Quezada (SEnTRA) : Se agrega validación de blanco en periodo de liquidación en modificación de datos
 *          para evitar inconsistencia de renta fija en miles de pesos y renta fija en pesos enviada al SGC.</li>
 *
 * <li> 3.2  02/05/2006  Nelly Reyes Quezada (SEnTRA) : Se cambian periodos tributarios cálculo Dai, año actual, menos 1 y menos 2 por
 *          año actual, menos uno y más uno.</li>
 *
 * <li> 4.0  01/03/2007  Osvaldo Lara Lira (SEnTRA) : Se modifica el manejo de errores de los métodos grabaCalculoRenta y modificaRentasCliente
 *           para poder lanzar el mensaje específico. Además se crea el método actualizaDatosHistoricos que borra los datos de la base de datos sybase
 *           cuando la inserción de los datos en IBM falla.</li>
 *           
 * <li> 4.1  26/06/2007  Manuel Durán Moyano (Imagemaker IT) : Se sobrecarga método getRangoPeriodo(int) para obtener un rango que abarque todas 
 *           las liquidaciones de sueldo de un cliente. Además se modifican ciertas referencias desde el antiguo método getRangoPeriodo(int) 
 *           al nuevo getRangoPeriodo() en los siguientes méodos: cargaLiquidacionesDeSueldo(RentaCliente,boolean),
 *           cargaBoletaHonorarios(RentaCliente,boolean) y cargaIngresosAdicionales(RentaCliente,boolean).</li>
 *  
 * <li> 4.2  31/07/2007  Manuel Durán Moyano (Imagemaker IT) : Se sobreescribe la renta del conyuge que provee ConsCliPerIBM del cliente por la suma
 *           de la rentaVariabla y rentaFija, obtenidas con el rut del conyuge desde ConsCliPerIBM en el método modificaRentasCliente(long,char,RentaCliente). 
 *           Además se agrega comprobación que rut del conyuge no sea vacío.
 *           Esta mantención es provisoria mientras se rediseñan servicios IBM que reemplazarán ConsCliPerIBM y CliCreaModClPer. </li>
 *
 * <li> 4.3  23/11/2007  Manuel Durán Moyano (Imagemaker IT) : Se agrega metodo actualizaRentasCliente(long,dvCliente,RentaCliente)
 *           el cual se basa en modificaRentasCliente(long,dvCliente,RentaCliente), esto debido al nuevo servicio de 
 *           grabado en SGC CliActuRenta que reemplazara a CliCreaModClPer, por lo que se cambia la llamada en
 *           grabaCalculoRenta() de modificaRentasCliente a actualizaRentasCliente. Se cambian periodos exigidos para no clientes,
 *           los que usaran la mismas reglas de los Clientes según D01 en método CalculaPeriodosExigidos(). Se cambia
 *           logs de writetolog y System.out.println a log4j. Finalmente se eliminan imports innecesarios.</li>
 *
 * <li> 4.4   07/04/2008, Vasco Ariza M. (SEnTRA.), Mauricio Retamal C. (SEnTRA): Se agregan metodos utilizados para obtencion y almacenamiento de cotizaciones de clientes a Previred y la posterior         
 *          actualización de renta calculada a partir de estas cotizaciones. Los metodos agregados son: obtenerRentaClientePersona(), obtenerValorUF(), consultaCotizacionCliente(),
 *          ingresoCotizacionCliente(), ingresaErrorCotizacion().</li>
 *
 * <li> 4.5  09/09/2008  Manuel Durán Moyano (Imagemaker IT) : Se cambia EJB de Stateful a Stateless, esto 
 *           implicó eliminar atributos de clase: isParametrosCargados, isPeriodosCargados, liquidaciones, 
 *           dai, boletas, EvaluacionCliente y parametrosCalculo junto con todas sus referencias. Para 
 *           mantener al máximo la lógica se creo un TO CalculoRentaTO, el que agrupa los atributos antes 
 *           mencionados, y es enviado como parametro cuando es necesario. Además se modifica log4j de apache
 *           y se limitó ancho de línea a 115 en métodos modificados, según nueva normativa.</li>
 *           
 * <li> 4.6  16/09/2008  Manuel Durán Moyano (Imagemaker IT) : Debido a mejoras en aplicación cálculo de ingresos
 *           se agrega método {@link #obtieneFechaPermanencia()} que es usado por 
 *           {@link #obtieneCalculoCliente((boolean,String,String,String,boolean,CalculoRentaTO))} para iniciar o
 *           retomar un cálculo de ingresos.</li>
 *          
 * <li> 4.7   01/09/2008  Evelin Sáez G.(Imagemaker IT) : Se modifica el método {@link #grabaCalculoRenta}, 
 *           en él se agrega el grabado de los datos de la renta de un cliente en el sistema TCorp y se realiza
 *           un par de mejoras. Además, se crea el método {@link #actualizaDatosTcorp(String, boolean)} y 
 *           {@link #tieneProductosTbanc(long)}.</li>
 *           
 * <li> 4.8   22/04/2009  Pedro Carmona Escobar (SEnTRA) : Se agregan los siguientes métodos relacionados con la lógica
 *           de negocios para la actualización de renta de un cliente mediante sus cotizaciones obtenidas desde de previred:<ul>
 *           <li>{@link #obtenerConfirmacionConsultaPrevired(long rut, char dv)}
 *           <li>{@link #actualizarRentaConCotizaciones(String canal, long rut, char dv, MultiEnvironment multiEnvironment)}
 *           <li>{@link #obtieneStringDesdeDocument(Document doc)}
 *           <li>{@link #obtenerInstanciaSeriviciosCliente()}
 *           <li>{@link #cargarDatosDetalleCotizacion(String xmlSalida)}
 *           <li>{@link #cargarDatosCotizacion(long rutCliente, char dvCliente, String canal, String xmlSalida, int identificadoRegistro)}
 *           <li>{@link #validarCotizacionCalculoRenta(String xmlSalida)}
 *           <li>{@link #validarErrorCotizacionCliente(String xmlSalida)}
 *           <li>{@link #generarEntradaConsultaPrevired(long rutCliente, char dvCliente, int idRegistro))}
 *           <li>{@link #cargarDatosCotizacionError(String canal, long rutCliente, char dvCliente, int identificadoRegistro)}
 *           <li>{@link #cargarDatosEncabezadoCotizacion(String canal, long rutCliente, char dvCliente)}
 *           <li>{@link #validarConsultaPrevired(double rentaActual, double valorUF)}
 *           <li>{@link #validarHorarioPrevired()}
 *           <li>{@link #estadoRenta(long rut, char dv, MultiEnvironment multiEnvironment)}
 *           <li>{@link #obtenerCotizaciones(String canal, long rut, char dv)}
 *           <li>{@link #actualizarRentaCalculada(long rut, char dv, CotizacionClienteVO cotizacionCliente, double rentaActualizada)}
 *           <li>{@link #actualizaEstadoEncabezado(int idRegistro, String estado)}
 *           <li>{@link #obtieneUltimoRegistroValidoDeCotizaciones(long rut, Date fechaValidez)}
 *           </ul>
 *           Se realiza este cambio para modularizar la lógica de negocios, con lo que se permite la reutilización de la misma cuando sea necesario.
 *           Además, se hace un reemplazo de la invocación al ServicioClientes en los métodos {@link #obtenerRentaClientePersona(long rut, char dv)} 
 *           , {@link #actualizaRentasCliente(long rutcliente, char dvCliente, RentaCliente rentaCliente)}  lo que implica también una modificación en el método {@link #ejbCreate()}. 
 *           Por último, se optimizan los import de la clase y se depreca el método {@link #modificaRentasCliente(long, char, RentaCliente)}.</li>
 *           
 * <li> 4.9  01/06/2009  Eugenio Contreras (Imagemaker IT) : Se modifica el método {@link #setLiquidacion},
 *           agregando un mayor filtro sobre las excepciones que pueden ocurrir dentro de éste.  De ésta manera
 *           se pueden devolver errores que digan por qué fallo el proceso.</li>
 * 
 * <li> 5.0  30/09/2009  Eugenio Contreras (Imagemaker IT) : Se reversan los cambios realizados en la versión 4.9
 *           ya que generaron un resultados no deseados en el ingreso de liquidación de clientes
 *           (método {@link #setLiquidacion}).</li>
 *
 * <li> 5.1  02/10/2009  Eugenio Contreras (Imagemaker IT): Se modifica y agregan líneas de log en los siguientes métodos:<br>
 *                       <ul>
 *                          <li>{@link #grabaCalculoRenta(RentaCliente)}</li>
 *                          <li>{@link #actualizaDatosHistoricos(String, String, int, int)}</li>
 *                          <li>{@link #actualizaRentasCliente(long, char, RentaCliente)}</li>
 *                          <li>{@link #tieneProductosTbanc(long)}</li>
 *                          <li></li>
 *                       </ul><br>
 *                       Se agregan sentencias isEnabledFor(Level.ERROR) de log4j, al momento de logear
 *                       excepciones en los métodos.
 * </li>
 * 
 * <li> 5.2  20/02/2010  Eugenio Contreras (Imagemaker IT): Se modifica el método: <br>
 *                       {@link #actualizaRentasCliente(long, char, RentaCliente)}<br>
 *                       Para aladir lógica que permita transformar la renta fija en formato
 *                       de pesos a miles cuando el origen de la renta sea por DAI.</li>
 * 
 * <li> 5.3 20/04/2010  Miguel Sepúlveda G. (Imagemaker IT.): Se modifican los siguientes metodos para restringir que el 
 *                      calculo de valores ingresados de una renta negativa:
 *                      <ul>   
 *                          <li>{@link #setDai(Dai, CalculoRentaTO)}</li>
 *                          <li>{@link #setLiquidacion(LiquidacionDeSueldo, CalculoRentaTO)}</li>
 *                          <li>{@link #CalculaIngresosPorLiquidacion(LiquidacionDeSueldo, CalculoRentaTO)}</li>
 *                          <li>{@link #CalculaIngresosPorDai(Dai, CalculoRentaTO)}</li>
 *                      </ul> 
 * <li> 5.4 25/05/2010    Roberto Rodríguez (Imagemaker IT): Se agregan logs a los métodos:<br>
 *                         {@link #grabaCalculoRenta(RentaCliente evaluacionCliente)}<br>
 *                         {@link #actualizaDatosHistoricos(String rut, String dv, int numeroSecuencia,int indicador)}
 *                         <br>
 *                         {@link #actualizaDatosTcorp(String rutCliente, boolean graboTcorp)}
 * <li> 5.5 02/07/2010  Paulina Vera M. (SEnTRA): Se modifican los métodos:
 *                                 {@link #actualizarRentaConCotizaciones(String canal, long rut, char dv, MultiEnvironment multiEnvironment, String codServicio)}
 *                                 {@link #cargarDatosEncabezadoCotizacion(String canal, long rutCliente, char dvCliente, String codServicio)}</li>
 * 
 * <li> 5.5 14/07/2010 Roberto Rodríguez (Imagemaker IT): Se actualizaron logs a los métodos:<br>
 *                         {@link #grabaCalculoRenta(RentaCliente evaluacionCliente)}<br>
 *                         {@link #actualizaDatosHistoricos(String rut, String dv, int numeroSecuencia,int indicador)}
 *                         <br>
 *                         {@link #actualizaDatosTcorp(String rutCliente, boolean graboTcorp)}
 *                         <br>                        
 *                         {@link #obtieneCalculoHistorico(RentaCliente evaluacionCliente)}
 * <li> 5.6 15/07/2010 Ximena Medel P. (SEnTRA): Se organizan imports por migración de weblogic 8.1 a weblogic 10.3
 * </li>
 * <li> 5.7  09/09/2010 Miguel Sepúlveda (Imagemaker IT): Se cambian logger del método {@link #actualizaRentasCliente(long, char, RentaCliente)}
 *                                                        esto se realiza para separar los archivos de calculo de renta y utilizar el definido en el
 *                                                        log CalculoRentaEspecifico.</li>
 * <li> 5.8 21/03/2011 Miguel Sepúlveda G. (Imagemaker IT): Se modifica el metodo {@link #actualizaRentasCliente(long, char, RentaCliente)} agregando el parametro 
 *      CodMov para realizar la actualización de renta del cliente, este nuevo parametro fue agregado al tuxedo CliActuRenta.
 * <li> 5.9 07/07/2011  Javier Aguirre A. (Imagemaker IT): Se agrega metodo {@link #obtenerConfirmacionConsultaPreviredFlag(long rut, char dv)} permite identificar si ha ocurrido una excepcion interna </li>
 * <li>5.10 08/08/2011 Cristoffer Morales L. (TINet): Se agrega el método 
 *          {@link #obtenerRentaConCotizacionesPrevired(String, long, char, String, DatosBiometricosTO)}
 *          para obtener las rentas desde Previred y además se publica el método 
 *          {@link #actualizaRentasCliente(long, char, RentaCliente) }
 *          
 * <li> 5.11  16/02/2011 Christopher Finch U. (Imagemaker IT): se agrega trim() en una  comparación del método
 *                                                        {@link #actualizaRentasCliente(long, char, RentaCliente)}</li>

 * <li>5.11 15/11/2011 Felipe Rivera C. (TINet): Se retira la lógica asociada al cálculo de la renta del
 * cliente en el método "obtenerRentaConCotizacionesPrevired" con el fin de mover la misma al método que realiza
 * las validaciones correspondientes y que este cálculo no se realice si es que las validaciones asociadas no se
 * cumplen.
 * <li>5.12 13/05/2013 Samuel Merino A. (Ada Ltda.): Se modifica el método {@link actualizarRentaConCotizaciones} 
 *                                                   en su lógica para invocar al utilitario que realiza el cálculo
 *                                                   de renta, para posteriormenete realizar la actualización de 
 *                                                   renta en caso de ser necesaria.</li>
*               indicados.</li>                                     
 * <li>6.0 15/04/2014 Pedro Rebolledo Lagno - Rodrigo Pino (SEnTRA): Se cambia modificador de la variable logger, 
 *                                   se crea método getLogger según la normativa y se crean los métodos:
 *                                       {@link #consultaUltimoCalculo(String, String)}
 *                                       {@link #eliminarBorradorRenta(CalculoRentaTO)} 
 *                                       {@link #consultarDetalleBienRaiz(long, char, int)}
 *                                       {@link #ingresarDetalleBienRaiz(long, char, int, DetalleArriendoTO)}
 *                                   Se modifican los métodos:
 *                                       {@link #obtieneParametrosCalculo(CalculoRentaTO)}
 *                                       {@link #CalculaIngresosPorDai(Dai, CalculoRentaTO)}
 *                                       {@link #calculoDaiConBoletas(Dai, CalculoRentaTO)}
 *                                       {@link #obtieneDai(String, String, CalculoRentaTO)}
 *                                    {@link #CalculaIngresosPorBoletas(RentaCliente, boolean, CalculoRentaTO)}
 *                                    {@link #CalculaIngresosPorLiquidacion(LiquidacionDeSueldo, CalculoRentaTO)}
 *                                    {@link #calculoRentaConLiquidacion(CalculoRentaTO)}
 *                                   Se agregan las variables:
 *                                       {@link #TOTAL_MESES}
 * </li>
* <li>6.1 07/08/2014 Eduardo Villagrán Morales (Imagemaker): Se modifican los métodos:
*               - {@link #actualizarRentaConCotizaciones(String, long, char, MultiEnvironment, String)}
*               - {@link #obtenerCotizaciones(String, long, char)} 
*               para solucionar problemas al actualizar renta.
*               Se agrega el método {@link #getLogger()}, se normalizan y mejoran logs de los 2 métodos arriba
*               indicados.</li>                                     
 * <li>7.0 16/09/2014 Pedro Carmona Escobar (SEnTRA):Se crean los métodos:
 *                  {@link #obtenerRentaConCotizacionesEquifax(String, long, char,String, String, String, Date)},
 *                  {@link #evaluarReglasEquifax(RentaSolicitanteTO, boolean)}. Además
 *                  se modifica el método {@link #actualizaRentasCliente(long, char, RentaCliente)}.
 * <li>8.0 29/09/2014 Pedro Rebolledo Lagno (SEnTRA):
 *                                   Se modifica el método:
 *                                    {@link #calculoDaiConBoletas(Dai, CalculoRentaTO)}
 *                                    {@link #CalculaIngresosPorLiquidacion(LiquidacionDeSueldo, CalculoRentaTO)}
 *                                    {@link #actualizaRentasCliente(long, char, RentaCliente)}
 *                                   Se agregan las variables:
 *                                       {@link #TOPE_VALIDACION_BOLETAS}
 *                                       {@link #DIA_MES_DAI}
 * </li>
 * <li>9.0 22/10/2014 Manuel Escárate R. (BEE): Se agrega variable MONTO_MULTI_RENTA y CANAL_NOVA.
 *                                   Se sobrecarga método {@link actualizarRentaConCotizaciones},
 *                                   Se modifica método {@link actualizarRentaCalculada},
 *                                   Se agrega los siguientes métodos : {@link validaRentaNova},
 *                                   {@link ingresarRegistroRTA}, {@link getClienteBean}.
 *                                   
 *</li>
 *<li>10.0 26/02/2015 Manuel Escárate R. (BEE): Se agregan variables para setear los montos en la 
 *                                              actualización de le renta.

 * <li>11.0 19/03/2015 Pedro Carmona Escobar, Alejandro Barra (SEnTRA) - Andrés Alvarado (Ing. Soft. BCI):Se modifica el método:
 *                  {@link #obtenerRentaConCotizacionesEquifax(String, long, char,String, String, String, Date)}.
 *                                se modifica el retorno, se elimina variable logCalculo, 
 *                            se agrega llamado a método getLogger, se normaliza log y
 *                                se modifica javadoc del método {@link #grabaCalculoRenta(RentaCliente).
 *                                se elimina la variable logCalculo, se agrega llamado a método getLogger,
 *                                se normaliza log, se modifica javadoc y se norma encabezado del método
 *                                {@link #obtieneCalculoHistorico(RentaCliente)}.
 *                                se elimina la variable logCalculo, se agrega llamado a método getLogger
 *                                y se normaliza log del método
 *                                {@link #actualizaDatosHistoricos(String, String, int, int)}.
 *                                se elimina la variable logCalculo, se agrega llamado a método getLogger
 *                                y se normaliza log del método 
 *                                {@link #actualizaRentasCliente(long, char, RentaCliente)}.
 *                                se elimina la variable logCalculo, se agrega llamado a método getLogger
 *                                y se normaliza log del método {@link #actualizaDatosTcorp(String, boolean)}.
 *            se agregan los métodos:
 *            {@link #grabaEncabezadoCalculoAutomatico(EncabezadoCalculoAutomaticoTO)}
 *            {@link #grabaDetallesCalculoAutomatico(DetalleCotizacionClienteVO[], EncabezadoCalculoAutomaticoTO)}
 *            {@link #consultaEncabezadoCalculoAutomatico(int)}
 *            {@link #consultaDetallesCalculoAutomatico(int)}
 *            se elimina la declaracion de la variable logCalculo
 * </li>
 * <li> 11.1 20/07/2015 Jorge San Martín (SEnTRA) - Paula León (Ing. Soft. BCI): Se modifica para llamada al método 
 * {@link CalculoRentaBean#actualizaRentasCliente()} para agregar el código de usuario a la llamada a método del ejb.
 * </li> 
 * <li>11.2 04/11/2015, José Luis Allende (ImageMaker IT)-Rodrigo Ortiz (ing. Soft. BCI): Se verifica que se haya 
 *                      realizado una modificación en el dai y liquidación antes de lanzar error por cálculo de ingreso negativo.</li>
 * </li>
 * <li> 11.3 15/03/2016 Andrés Cea S. (TINet); Ignacio González D. (TINet) - Oliver Hidalgo (Ing. Soft. BCI): 
 *      Se agregan los siguientes métodos:
 * <ul>
 *        <li>{@link #actualizarRentaClienteCotizaciones(long, char, String, String)}</li>
 *        <li>{@link #mapeaRentaTOACotizacionClienteVO(RentaTO)}</li>
 *        <li>{@link #mapeaCotizacionClienteVOARentaTO(CotizacionClienteVO)}</li>
 *        <li>{@link #mapeaCotizacionTOADetalleCotizacionClienteVO(CotizacionTO)}</li>
 *        <li>{@link #mapeaDetalleCotizacionClienteVOACotizacionTO(DetalleCotizacionClienteVO)}</li>
 *        <li>{@link #actualizarRentaProspectoViaje(long, char, String, String, int)}</li>
 *        <li>{@link #actualizarRentaProspectoTransfer(long, char, String, String, int)}</li>
 *        <li>{@link #actualizarRentaProspectoCotizaciones(long, char, String, String)}</li>
 *        <li>{@link #obtenerDetalleCotizacionesPrevired(long, char, String, int)}</li>
 *        <li>{@link #obtenerServiciosDePagosMasivos()}</li>
 *        <li>{@link #obtieneFechaMesesAnterior(int)}</li>
 * </ul>
 * </li>
 * <li> 11.4 22/04/2016 Ignacio González D. (TINet) - Oliver Hidalgo (Ing. Soft. BCI): Se modifican los métodos:
 * - {@link #actualizarRentaProspectoTransfer(long, char, String, String, int)}
 * - {@link #obtieneFechaMesesAnterior}
 * Se agrega método:
 * - {@link #obtieneFechaUltimoDiaDelMesActual()}</li>
 * <li> 11.5 24/05/2016 Rafael Pizarro (TINet); Hernán Rodríguez (TINet) - Claudia López (Ing. Soft. BCI): 
 *      Se modifican los siguientes métodos:
 * <ul>
 *        <li>{@link #actualizarRentaClienteCotizaciones(long, char, String, String)}</li>
 *        <li>{@link #obtenerDetalleCotizacionesPrevired(long, char, String, int)}</li>
 *        <li>{@link #mapeaRentaTOACotizacionClienteVO(RentaTO)}</li>
 *      Se agrega metodo:
 *        <li>{@link #cargarDatosCotizacionError(String, long, char, int, String, String)}</li>
 *        <li>{@link #registrarInformacionAdicionalError(Exception, String, long, char, int, String, String)}</li>
 *        <li>{@link #mapeaCotizacionTOADetalleCotizacionClienteVO(CotizacionTO)}</li>
 *        <li>{@link #getEjbAntecedentesCliente()</li>
 * </ul>
 * </li>
 *
 * <li> 12.0 01/08/2016 Ariel Acuña (SEnTRA) - Sergio Bravo (Ing. Soft. BCI): se agregan los métodos:
 *              {@link #ingresarRegistroActualizacionRenta(RegistroActualizaRentaTO)}
 * </li>
 * </ul><p>
 *
 * <b>Todos los derechos reservados por Banco de Crédito e Inversiones.</b>
 * <p>
 */
public class CalculoRentaBean implements javax.ejb.SessionBean {

    /**
     * Constante con tipo de evaluador cliente.
     */
    private static final String TIPO_CLIENTE = "cliente";

    /**
     * Constante con tipo de evaluador prospecto.
     */
    private static final String TIPO_PROSPECTO = "prospecto";
    
    /**
     * Constante con tipo de evaluador prospecto.
     */
    private static final String TIPO_PROSPECTO_TRANSFER = "prospectoTransfer";
    
    /**
     * Constante con tipo de cliente persona en sgc.
     */
    private static final String TIPO_CLIENTE_PERSONA_SGC = "P";
    
    /**
     * Constante con Formato de fecha MMyyyy.
     */
	private static final String FORMATO_FECHA_MMYYYY = "MMyyyy";
	
	/**
	 * Constante con código de error para caso en que el servicio de previred presente problemas.
	 */
	private static final String ERROR_SIN_SERVICIO_PREVIRED = "ERRACTREN01";

	/**
	 * Constante con código de error para caso en que no se pueda insertar cabecera en bdd de prospectos.
	 */
	private static final String ERROR_INSERT_CABECERA = "ERRACTREN02";

	/**
	 * Constante con código de error para caso en que no se pueda insertar renta en bdd de prospectos.
	 */
	private static final String ERROR_INSERT_RENTA_CALCULADA = "ERRACTREN03";
	
	/**
     * Constante con código de error para caso en que no se pueda obtener una fecha a un periodo anterior.
     */
    private static final String ERROR_FECHA_ANTERIOR = "ERRACTREN04";
    
    /**
     * Constante con código de error para caso en que no se pueda obtener una fecha solicitada.
     */
    private static final String ERROR_FECHA_SOLICITADA = "ERRACTREN08";
		
	/**
     * Constante con código de error para caso en que se opere previred en fuera de horario establecido.
     */
    private static final String ERROR_PREVIRED_FUERA_DE_HORARIO = "ERRPREVIRED01";
    
    /**
     * Constante con código de error para caso en que exista error al consultar horario de atención en previred.
     */
    private static final String ERROR_VALIDA_SERVICIO_PREVIRED = "ERRPREVIRED02";
    
    /**
     * Constante con código de error para caso en que exista restricción de acceso a previred.
     */
    private static final String ERROR_SIN_ACCESO_PREVIRED = "ERRPREVIRED03";
    
    /**
     * Constante con código de error para caso en que no existan pagos.
     */
    private static final String ERROR_SIN_PAGOS_DISPONIBLES = "ERRCALRENTRAN02";
    
    /**
     * Constante con código de error para caso en que no exista renta para calculo.
     */
    private static final String ERROR_SIN_RENTA = "ERRCALREN00";

    /**
     * Archivo de parámetros.
     */
    private static final String TABLA_PARAMETROS = "solicitudInstantanea.parametros";

    /**
     * Archivo de parámetros cálculo renta.
     */
    private static final String TABLA_PARAMETROS_CALCULORENTA = "CalculoRenta.parametros";
    
    /**
     * Valor que representan cantidad de boletas solicitadas.
     */
    private static final int TOPE_VALIDACION_BOLETAS = 3;
    
	/**
     * Valor del día y mes a concatenar.
     */
    private static final String DIA_MES_DAI = "0104";
	
    /**
     * Atributo que representa el canal.
     */
     private static final String CANAL_NOVA = "800";
    
     /**
      * Variable para obtener los miles.
      */
     private static final int MONTO_MULTI_RENTA = 1000;
     
     /**
      * Variable detalle error.
      */
     private static final int DET_ERROR_COT = 250;
   
    /**
     * Valor que representan cantidad de dias imponibles.
     */
    private static final int TOTAL_MESES = 12;
    
    /**
     * Valor de períodos exigidos.
     */
    private static final int PERIODOS_EXIGIDOS=3;
    /**
     * Valor para calculo de CV.
     */
    private static final int VALOR_CV=11;
    
    /**
     * Valor que representan el 80% para calcular.
     */
    private static final double PORCENTAJE_RENTA = 0.8;
    
    /**
     * Valor que representan cantidad de meses imponibles.
     */
    private static final int CANTIDAD_DIAS = 30;
    
    /**
     * Identificador de Calculo como Borrador.
     */
    private static final String ES_BORRADOR = "S";
    
    /**
     * Identificador de Calculo como NO aprobado por JOF.
     */
    private static final String NO_APROBADO_JOF = "N";

    /**
     * Identificador del Factor de Miles.
     */
    private static final int FACTOR_MILES = 1000;

    /**
     * Identificador del Formato Decimal.
     */
    private static final String FORMATO_DECIMAL = "###0";
    
    /**
     * Valor que representa el codigo de la consulta realizada sin error a Previred.
     */
    private static final String CODIGO_CONSULTA_EXITOSA_PREVIRED = "9050";
    
    /**
     * Constante para definir miles.
     */
    private static final int MILES = 1000;
    
    /**
     * Constante con código de respuesta servicio actualizacion renta sgc previred.
     * Actualización de renta realizada con éxito.
     */
    private static final String RESPUESTA_ACT_RENTA_0000 = "0000";
    
    /**
     * Constante con código de respuesta servicio actualizacion renta sgc previred.
     * Actualización de renta se encuentra actualizada, periodo 3 meses.
     */
    private static final String RESPUESTA_ACT_RENTA_0001 = "0001";
    
    /**
     * Constante con código de respuesta servicio actualizacion renta sgc previred.
     * No se puede crear registro para actualizar renta.
     */
    private static final String RESPUESTA_ACT_RENTA_0002 = "0002";
    
    /**
     * Constante con código de respuesta servicio actualizacion renta sgc previred.
     * No se dispone de detalle de cotizaciones.
     */
    private static final String RESPUESTA_ACT_RENTA_0003 = "0003";
    
    /**
     * Constante con código de respuesta servicio actualizacion renta sgc previred.
     * Renta no calculada.
     */
    private static final String RESPUESTA_ACT_RENTA_0004 = "0004";
    
    /**
     * Constante con código de respuesta servicio actualizacion renta sgc previred.
     * No se puede actualizar renta en SGC, Intente nuevamente.
     */
    private static final String RESPUESTA_ACT_RENTA_0005 = "0005";
    
    /**
     * Constante con código de respuesta servicio actualizacion renta sgc previred.
     * No se puede guardar renta actualizada en cotizacli.;
     */
    private static final String RESPUESTA_ACT_RENTA_0006 = "0006";
    
    /**
     * Constante con código de respuesta servicio actualizacion renta sgc previred.
     * No se puede guardar renta actualizada en dgc.;
     */
    private static final String RESPUESTA_ACT_RENTA_0007 = "0007";
    
    /**
     * Constante con código de error generico.
     * Sistema no disponible.;
     */
    private static final String ERROR_GENERICO_0002 = "0002";
    
    /**
     * Constante con código de error generico.
     * No existe registro del cliente.;
     */
    private static final String ERROR_GENERICO_0025 = "0025";
    
    /**
     * Constante con código de error generico.
     * RUT no existe.;
     */
    private static final String ERROR_GENERICO_0073 = "0073";
    
    /**
     * Cantidad de meses por el que se valida si la informacion esta actualizada en SGC.
     */
    private static final int MESES_ANTIGUEDAD_SGC = -3;
    
    /**
     * Nombre aplicacion que actualiza renta.
     */
    private static final String APLICACION_CORE_CALCULO_RENTA = "CoreCalculoRenta";
    
    /**
     * Constante que contiene el JNDI del Ejb AntecedentesClientes.
     */
    private static final String JNDI_ANTECEDENTES_CLIENTE = 
                                        "wcorp.aplicaciones.clientes.informaciondecliente.antecedentesclientes.implementation.AntecedentesClientes";
    
    /**
     * Constante que indica cliente sin antecedentes economicos.
     */
    private static final String CLIENTE_SIN_ANTECEDENTES_ECONOMICOS = "CLIENTE SIN ANTECEDENTES ECONOMICOS";
    
    /**
     * Bean de ServicioCalculoRenta
     */
    private ServicioCalculoRenta trxBean = null;

    /**
     * Home de ServicioCalculoRenta
     */
    private ServicioCalculoRentaHome trxHome = null;

    javax.ejb.SessionContext ejbSessionContext = null;

    /**
     * Log de la clase, para org.apache.log4j.Logger.
     */
    private transient Logger log = (Logger) Logger.getLogger( CalculoRentaBean.class );

    /**
     * Tabla de parámetros de la clase
     */
    private String parametrosNombreArchivo = "CalculoRenta.parametros";
    private static ServiciosCliente servCli;
    private ServiciosEconomia servEconomia;
    private static final String ARCHIVO_PARAMETROS ="previred/cotizacionCliente.parametros";    

    /**
     * Valor que representa basura dentro xml retornado por servipag, la cual debe ser retirada.
     */
    private static final String TAG_BASURA_1 = "#text";

    /**
     * Valor que representa basura dentro xml retornado por servipag, la cual debe ser retirada.
     */
    private static final String TAG_BASURA_2 = "#comment";

    /**
     * Valor que representan cantidad de cotizaciones (en períodos contínuos) necesarias para la actualización de renta.
     */
    private static final int CANTIDAD_COTIZACIONES = 12;

    /**
     * Formato fecha dd/MM/yyyy.
     * @since 6.1
     */
    private static final String FORMATO_FECHA_DD_MM_YYYY = "dd/MM/yyyy";

    /**
     * Atributo que contiene la instancia remota del ejb {@link AntecedentesClientes}.
     */
    private transient AntecedentesClientes antecedentesClientes;
    
    /**
     * <p> Setea nuevo cálculo</p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>1.1  09/09/2008, Manuel Durán M. (Imagemaker IT): Deprecado por cambios de staful a stateless,
     *          el seteo de nuevo cálculo se realizará en servlet despues de haber grabado renta.</li>
     * </ul>
     * 
     * @since ?
     * @deprecated Se debe setear nuevo cálculo directamende desde servlet que llama al proceso de grabado
     */  
    private void seteaNuevoCalculo() {

    }

    public String getEcho() {
        return "CalculoRentaBean";
    }

    /**
     * Consulta parámetros para cálculo de renta
     * <p>
     * Registro de versiones:<ul>
     * <li> 1.0 ??/??/???? desconocido - version inicial
     *
     * <li> 2.0 02/05/2006 Nelly Reyes Quezada, SEnTRA - se modifican periodos tributarios para cálculo de dai</li>
     * <li> 2.1 09/09/2008, Manuel Durán M. (Imagemaker IT): Se modifica interfaz, añandiendo y retornando el
     *          TO de cálculo de renta, debido al cambio de stateful a stateless. Además se migran logs 
     *          a log4j de apache.</li>
     * <li> 3.0 15/04/2014 Pedro Rebolledo Lagno (SEnTRA):
     *                                        Se ajustan las líneas de logueo según la Normativa Vigente. 
     *                                        Se agrega la obtención de los parametros correspondientes
     *                                        a: - Las AFP 
     *                                           - Las Ramas de las FFAA 
     *                                           - Los Rangos de las FFAA 
     * 
     * </li>
     * </ul>
     * <p>
     * @param rentaTO TO de cálculo de renta
     * @return TO de cálculo de renta con parámetros de cálculo cargados

     * @throws wcorp.serv.renta.CalculoRentaException
     * @since 3.1
     */
    public CalculoRentaTO obtieneParametrosCalculo(CalculoRentaTO rentaTO) throws CalculoRentaException {

        getLogger().debug("[CalculoRentaBean.obtieneParametrosCalculo()  -- ] Se ejecuta servicio");

        Constantes cDetalle = new Constantes();

        if (rentaTO.isParametrosCargados()) {
            getLogger().debug("[CalculoRentaBean.obtieneParametrosCalculo()--]Parametros se encuentras cargados");
            return rentaTO;
        }
        try {
            cDetalle.setCnyTrabaja(trxBean.obtieneParametrosGenerales("CnyTrabaja"));
            cDetalle.setCnyNoTrabaja(trxBean.obtieneParametrosGenerales("CnyNoTrabaja"));
            cDetalle.setGradoFFAA(trxBean.obtieneParametrosGenerales("Grado FFAA"));
            cDetalle.setRamasFFAA(trxBean.obtieneParametrosGenerales("Ramas FFAA"));
            cDetalle.setRangoIngresoFFAA(trxBean.obtieneParametrosGenerales("Rango FFAA"));
            cDetalle.setCVBoleta(trxBean.obtieneParametrosGenerales("CV Boleta"));
            cDetalle.setMesesBH(trxBean.obtieneParametrosGenerales("Meses BH"));
            cDetalle.setListadoAFP(trxBean.obtieneParametrosGenerales("AFP"));
        } catch (Exception ex) {
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[CalculoRentaBean.obtieneParametrosCalculo()--]Ocurrio una excepcion, esta es:"
                        + ex.getMessage());
            }
            throw new CalculoRentaException(ex.getMessage());
        }

        try {
            cDetalle.setVariabilidad(trxBean.obtieneParametrosRenta("Variabilidad"));
            cDetalle.setPctFijoRLM(trxBean.obtieneParametrosRenta("PCT fjo RLM"));
            cDetalle.setRVariable(trxBean.obtieneParametrosRenta("R. Variable"));
            cDetalle.setVariacionBH(trxBean.obtieneParametrosRenta("Variacion BH"));
            cDetalle.setDiaLimite(trxBean.obtieneParametrosRenta("Dia limite"));
            cDetalle.setPonderadorRenta(trxBean.obtieneParametrosRenta("pond_rta"));
        } catch (Exception ex) {
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[CalculoRentaBean.obtieneParametrosCalculo()--] Ocurrio una excepcion, esta es"
                        + ex.getMessage());
            }
            throw new CalculoRentaException(ex.getMessage());
        }

        String[] anos = { getPeriodoDai(getSaltoPeriodoDai() + 1), getPeriodoDai(getSaltoPeriodoDai()),
                getPeriodoDai(getSaltoPeriodoDai() - 1) };
        cDetalle.setAnoTributario(anos);

        rentaTO.setParametrosCargados(true);
        rentaTO.setParametrosCalculo(cDetalle);

        return rentaTO;
    }


    /**
     * <p> Indica si día actual es anterior al día limite</p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>1.1  09/09/2008, Manuel Durán M. (Imagemaker IT): Se modifica interfaz, añadiendo parámetros de
     *          cálculo, debido al cambio de stateful a stateless. Además se agrega javadoc</li>
     * </ul>
     * @param parametrosCalculo parámetros de cálculo de renta
     * @return booleano que indica si dia actual es depues de dia limite
     * @throws wcorp.serv.renta.CalculoRentaException
     * @since ? 
     */ 
    private boolean antesDe15dias(Constantes parametrosCalculo) throws CalculoRentaException {

        Calendar rightNow2 = Calendar.getInstance();
        rightNow2.setTime(new Date());
        if (log.isDebugEnabled()) {
            log.debug("rightNow2.get(Calendar.MONTH): " + rightNow2.get(Calendar.MONTH));
        }

        int diaLimite = 0;
        String sDiaLimite = String.valueOf(parametrosCalculo.getDiaLimite());
        sDiaLimite = sDiaLimite.substring(0, sDiaLimite.length() - 2);

        try {
            diaLimite = Integer.parseInt(sDiaLimite);
            if (log.isDebugEnabled()) {
                log.debug("OJO Dia Limite " + parametrosCalculo.getDiaLimite() + ", Formateado: " + diaLimite);
            }
        } catch (Exception ex) {
            if (log.isEnabledFor(Level.ERROR)) {
                log.error("Problemas (bprocess.CalculoRenta.obtieneParametrosDetalle) " + ex.getMessage());
            }
            throw new CalculoRentaException(ex.getMessage());
        }
        if (log.isDebugEnabled()) {
            log.debug("periodo actual: " + getPeriodoActualLiquidacion());
        }
        return (rightNow2.get(Calendar.DAY_OF_MONTH) <= diaLimite);
    }

    /**
     * <p> Obtiene informacion de renta del periodo</p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>1.1  09/09/2008, Manuel Durán M. (Imagemaker IT): Se modifica interfaz, añadiendo y retornando el
     *          TO de cálculo de renta, debido al cambio de stateful a stateless. Se setea el rut y nombre del
     *          empleador directamente y no en metodo obtieneLiquidacionesDelPeriodoDespues15, para no
     *          modificar interfaz de dicho método. Se elimina código comentado,Además se agrega javadoc.
     * </ul>
     * @param rutCliente rut del cliente
     * @param dvRut dígito verificador del cliente
     * @param reglaDe15dias indicador si aplica regla de 15 dias
     * @param rentaTO TO de cálculo de renta
     * @return TO de cálculo de renta con informacion del periodo
     * @since ? 
     */ 
    private CalculoRentaTO obtieneInformacionDelPeriodo(String rutCliente, String dvRut, boolean reglaDe15dias,
            CalculoRentaTO rentaTO) {

        /*
         * regla15dias = true significa que estamos despues del 15, pero no se
         * completara infomacion del me anterior
         */

        log.debug("bprocess.CalculoRenta.obtieneInformacionDelPeriodo");
        // LIQUIDACIONES DEL PERIODO

        LiquidacionDeSueldo[] liquidaciones = null;
        Dai[] dai = null;
        BoletaHonorarios[] boletas = null;
        try {
            log.debug("**************LIQUIDACIONES***************");
            if (antesDe15dias(rentaTO.getParametrosCalculo()))// antes del dia
                // limite
                liquidaciones = obtieneLiquidacionesDelPeriodoAntes15(rutCliente, dvRut);
            else // despues del dia limite
                if (reglaDe15dias)
                    liquidaciones = obtieneLiquidacionesDelPeriodoDespues15(rutCliente, dvRut);
                else
                    liquidaciones = obtieneLiquidacionesDelPeriodoAntes15(rutCliente, dvRut);
            rentaTO.setLiquidaciones(liquidaciones);
            int len = liquidaciones == null ? 0 : liquidaciones.length;
            if (len > 0) {
                rentaTO.getEvaluacionCliente().setRutEmpleador(liquidaciones[0].getRutEmpresa());
                rentaTO.getEvaluacionCliente().setNombreEmpleador(liquidaciones[0].getRazonSocialEmpresa());
            }

            log.debug("**************DAI***************");
            dai = obtieneDaiDelPeriodo(rutCliente, dvRut);
            rentaTO.setDai(dai);

            log.debug("**************BOLETAS***************");
            boletas = obtieneBoletasDelPeriodo(rutCliente, dvRut);
            rentaTO.setBoletas(boletas);

        } catch (Exception ex) {
            if( log.isEnabledFor(Level.ERROR) ){
                log.error("[obtieneInformacionDelPeriodo] Ha ocurrido un error: " + ex);
            }
        }
        rentaTO.setPeriodosCargados(true);

        log.debug("**************FIN*****************************");
        log.debug("bprocess.CalculoRenta.obtieneInformacionDelPeriodo");
        log.debug("***************************************************\n\n\n\n");
        return rentaTO;
    }


    /**
     * Graba la renta tanto en sybase como en el sistema general de clientes
     * <p>
     *
     * Registro de versiones:<ul>
     *
     * <li> 1.0 ??/??/???? Desconocido : versión inicial.</li>
     * <li> 1.0 01/03/2007 Osvaldo Lara Lira(SEnTRA) : Se hace un mejor manejo de errores para desplegar el mensaje exacto al ejecutivo
     *          de la causa porque falló la inserción de datos. Además se llama al método actualizaDatosHistoricos para borrar la
     *          información temporal de datos si es que la inserción en el sistema general de clientes fue correcta. Si el ingreso
     *          de datos a IBm falla entonces el método mencionado borra los datos de sybase para dejar todo el proceso consistente.</li>
     * <li> 1.2 23/11/2007, Manuel Durán M. (Imagemaker IT): Se cambia llamada a nuevo metodo de grabacion en SGC, de
     *          modificaRentasCliente() a actualizaRentasCliente(), debido a nuevo servicio de grabado CliActuRenta</li>
     * <li> 1.3 09/09/2008, Manuel Durán M. (Imagemaker IT): Se modifica interfaz, añadiendo la evaluacion del 
     *          cliente  debido al cambio de stateful a stateless. Se elimina seteo de origen de cálculo en 
     *          atributo de clase y el seteo de un nuevo cálculo ya que se realiza en servlet GrabaCalculo 
     *          sobre el TO.</li>
     * <li> 1.4 01/09/2008, Evelin Sáez G. (Imagemaker IT) : Se graba en sistema Tcorp la renta del cliente,
     *          únicamente si éste tiene productos Tbanc. El nuevo orden de grabado será: sybase(histórico), 
     *          Tcorp, SGC(IBM). Con el nuevo cambio, si no graba correctamente en SGC, se hará un rollback 
     *          tanto en tcorp como en sybase(histórico) con el fin de uniformar el ingreso de rentas. Además,
     *          como mejora, se reemplaza el uso de la clase DigitoVerificador por la clase RUTUtil y se vuelve
     *          a los datos anteriores del histórico cuando falla el servicio que trae los datos del cliente BCI.</li>
     * <li> 1.5 02/10/2009, Eugenio Contreras (Imagemaker IT): Se agregan líneas de log. </li>
     * <li> 1.6 25/05/2010, Roberto Rodríguez(Imagemaker IT): Se agregan líneas de log con el fin  de sacarlos a un
     *             archivo diferente </li>
     *    <li> 1.2 14/07/2010, Roberto Rodríguez(Imagemaker IT): Se cambia la forma de comprobar si es un cliente,
     *cambiando la invocacion de wcorp.model.actores.cliente por preguntar a la renta si es que es cliente.
     * Se agrega más información en los logs </li> 
     * <li> 1.8 19/03/2015 Alejandro Barra (SEnTRA) - Andrés Alvarado (Ing. Soft. BCI):se modifica el retorno, se elimina variable logCalculo, 
     *                            se agrega llamado a método getLogger, se normaliza log y
     *                            se modifica javadoc del método .
     * </ul><p>
     * @param evaluacionCliente evaluacion de renta a grabar
     * @return un boolean que indica si la operación fue o no exitosa
     * @throws TuxedoException en caso de haber algun error en el tuxedo.
     * @throws CalculoRentaException  en caso de haber algun error en el cálculo de renta.
     * @throws ClientesException en caso de haber algun error en la actualización del cliente.
     * @throws GeneralException en caso de haber algun error.
     * @throws RemoteException en caso de haber algun error.
     * @throws NamingException en caso de haber algun error.
     * @throws CreateException en caso de haber algun error.
     * @since 1.0
     */
    public ResultadoRegistroRentaTO grabaCalculoRenta(RentaCliente evaluacionCliente) throws TuxedoException,
    CalculoRentaException, ClientesException, GeneralException, RemoteException, NamingException,
    CreateException {
        if (getLogger().isEnabledFor(Level.INFO)){
            getLogger().info("[grabaCalculoRenta][BCI_INI] inicio");
        }
        int respuestaServicio = 0;
        ResultadoRegistroRentaTO resultadoRegistroRenta = new ResultadoRegistroRentaTO();
        try {
            if (getLogger().isEnabledFor(Level.DEBUG)){
                getLogger().debug("[grabaCalculoRenta] Renta a recibida como parámetro: {" 
                                   + evaluacionCliente.toString() + "}");
                getLogger().debug("[grabaCalculoRenta] Usuario Actualiza: " 
                                   + evaluacionCliente.getUsuarioActualiza());
                getLogger().debug("[grabaCalculoRenta] Usuario Ingresa: " 
                                   + evaluacionCliente.getUsuarioIngreso());
            }            
            if (getLogger().isEnabledFor(Level.DEBUG)){
                getLogger().debug("[grabaCalculoRenta] llamando a método: "
                        + "ServicioCalculoRentaBean#modificaCalculoRenta");
            }            
            respuestaServicio = trxBean.modificaCalculoRenta(evaluacionCliente);
            if (getLogger().isEnabledFor(Level.DEBUG)){
                getLogger().debug("[grabaCalculoRenta] ejecución completa de método: "
                        + "ServicioCalculoRentaBean#modificaCalculoRenta " + respuestaServicio);
            }
            
            resultadoRegistroRenta.setSecuencia(respuestaServicio);

            if (getLogger().isEnabledFor(Level.DEBUG)){
                getLogger().debug("[grabaCalculoRenta] Respuesta de método "
                                   + "ServicioCalculoRentaBean#modificaCalculoRenta: {" + respuestaServicio + "}");
            }            
            }
        catch (Exception ex) {
            if (getLogger().isEnabledFor(Level.ERROR)){
                getLogger().error("[grabaCalculoRenta][BCI_FINEX][Exception] error con mensaje: "
                                   + ex.getMessage(), ex);
            }            
       
            throw new CalculoRentaException(ex.getMessage());
        }

        long rutCliente = Long.parseLong(evaluacionCliente.getRutCliente());
        String dv = evaluacionCliente.getDvRut();
        if (dv == null || dv.trim().equals("")) {
            if (getLogger().isEnabledFor(Level.DEBUG)){
                getLogger().debug("[grabaCalculoRenta] Calculando el dígito verificador de rut de cliente");
            }
            dv = "" + RUTUtil.calculaDigitoVerificador(rutCliente);

        }
        if (getLogger().isEnabledFor(Level.DEBUG)){
            getLogger().debug("[grabaCalculoRenta] Se obtiene RUT de Cliente, RUT: {"
                                    + rutCliente + "-" + dv + "}");
        }

        boolean esClienteBCI = evaluacionCliente.isEsClienteBci();
        
        if (getLogger().isEnabledFor(Level.DEBUG)){
            getLogger().debug("[grabaCalculoRenta] {" + rutCliente + "-" + dv + "} esClienteBCI {"
                                    + esClienteBCI + "}");
        }
        
        if (esClienteBCI) {
            // Graba en TCorp            
            if (getLogger().isEnabledFor(Level.DEBUG)){
                getLogger().debug("[grabaCalculoRenta] Verificando si cliente BCI tiene productos TBANC");
            }
            boolean graboTcorp = false;
            boolean tieneProdTbanc = false;

            try {
                if (getLogger().isEnabledFor(Level.DEBUG)){
                    getLogger().debug("[grabaCalculoRenta] Llamando a #tieneProductosTbanc");
                }
                tieneProdTbanc = tieneProductosTbanc(rutCliente); 
                if (getLogger().isEnabledFor(Level.DEBUG)){
                getLogger().debug("[grabaCalculoRenta] Verificación de productos TBANC realizada correctamente");
                }
            }
            catch (GeneralException e) {
                
                if (getLogger().isEnabledFor(Level.ERROR) ){
                    getLogger().error("[grabaCalculoRenta] Error al verificar si cliente "
                                       + "BCI tiene productos TBANC.");
                    getLogger().error("[grabaCalculoRenta] [Exception]", e); 
                    getLogger().error("[grabaCalculoRenta] Se realizará rollback");
                    getLogger().error("[grabaCalculoRenta] Parámetros enviados:");
                    getLogger().error("[grabaCalculoRenta] RUT = {" + evaluacionCliente.getRutCliente() + "}");
                    getLogger().error("[grabaCalculoRenta] DvRut = {" + evaluacionCliente.getDvRut() + "}");
                    getLogger().error("[grabaCalculoRenta] Núm. Secuencia = {" + respuestaServicio + "}");
                    getLogger().error("[grabaCalculoRenta] Indicador(1=falla;0=OK) = {1}");
                }

                actualizaDatosHistoricos(String.valueOf(evaluacionCliente.getRutCliente()),
                        String.valueOf(evaluacionCliente.getDvRut()), respuestaServicio, 1);
                if (getLogger().isEnabledFor(Level.ERROR) ){
                    getLogger().error("[grabaCalculoRenta] Rollback se realizó correctamente.");
                }
                if (getLogger().isEnabledFor(Level.ERROR)){
                    getLogger().error("[grabaCalculoRenta][BCI_FINEX][GeneralException] error con mensaje: "
                                       + e.getMessage(), e);
                }
                throw e;
            }
            if (getLogger().isEnabledFor(Level.DEBUG)){
                getLogger().debug("[grabaCalculoRenta] Tiene productos TBANC? = " + tieneProdTbanc);
            }            

            if (tieneProdTbanc) {
                try {            
                    if (getLogger().isEnabledFor(Level.DEBUG)){
                        getLogger().debug("[grabaCalculoRenta] Ingresando Rentas en TCORP");
                        getLogger().debug("[grabaCalculoRenta] Se llama al método: "
                                          + "ServicioCalculoRentaBean#ingresaCalculoRentaTbanc");
                    }
                    if (getLogger().isEnabledFor(Level.DEBUG)){
                        getLogger().debug("[grabaCalculoRenta] Se envía como parámetro la renta: {"
                                          + evaluacionCliente.toString() + "}");
                    }
                    graboTcorp = trxBean.ingresaCalculoRentaTbanc(evaluacionCliente);
                    if (getLogger().isEnabledFor(Level.DEBUG)){
                        getLogger().debug("[grabaCalculoRenta] La renta del cliente fue "
                                          + "ingresada correctamente en TCORP");
                    }
                }
                catch (GeneralException e) {
                    
                    if (getLogger().isEnabledFor(Level.ERROR) ){
                        getLogger().error("[grabaCalculoRenta] Problemas al ingresar la renta del "
                                          + "cliente en TCORP");
                        getLogger().error("[grabaCalculoRenta] [Exception]", e);
                        getLogger().error("[grabaCalculoRenta] Se realizará rollback");
                        getLogger().error("[grabaCalculoRenta] Parámetros enviados:");
                        getLogger().error("[grabaCalculoRenta] RUT = {" + evaluacionCliente.getRutCliente() + "}");
                        getLogger().error("[grabaCalculoRenta] DvRut = {" + evaluacionCliente.getDvRut() + "}");
                        getLogger().error("[grabaCalculoRenta] Núm. Secuencia = {" + respuestaServicio + "}");
                        getLogger().error("[grabaCalculoRenta] Indicador(1=falla;0=OK) = {1}");
                    }                    
                    actualizaDatosHistoricos(String.valueOf(evaluacionCliente.getRutCliente()),
                            String.valueOf(evaluacionCliente.getDvRut()), respuestaServicio, 1);
                    if(getLogger().isEnabledFor(Level.ERROR)){
                        getLogger().error("[grabaCalculoRenta] Actualizó datos históricos");
                    }
                    if(getLogger().isEnabledFor(Level.ERROR)){
                        getLogger().error("[grabaCalculoRenta][BCI_FINEX][GeneralException] error con mensaje: "
                                           + e.getMessage(), e);
                    }
                    throw e;
                }           
            } 


            // HAY QUE GRABAR EN SISTEMA GENERAL DE CLIENTES.....
            // ACTUALIZACION SGC INICIO
            // Graba SGC
            if (getLogger().isEnabledFor(Level.DEBUG)){
                getLogger().debug("[grabaCalculoRenta] El Cliente: " + evaluacionCliente.getRutCliente() + "-"
                        + evaluacionCliente.getDvRut()
                        + " es cliente BCI y debe actualizarse la informacion de renta");
                getLogger().debug("[grabaCalculoRenta] Renta Fija: " + evaluacionCliente.getRentaFija());
                getLogger().debug("[grabaCalculoRenta] Renta Variable: " + evaluacionCliente.getRentaVariable());
            }
            try {
                // Se cambia la llamada para usar nuevo servicio Tuxedo
                // CliActuRenta
                // boolean grabaCliente = modificaRentasCliente(rutCliente,
                // dv.charAt(0), EvaluacionCliente);
                
                if(getLogger().isEnabledFor(Level.DEBUG)){
                    getLogger().debug("[grabaCalculoRenta] Se llama a método: "
                                       + "#actualizaRentasCliente, para actualizar renta en SGC");
                    getLogger().debug("[grabaCalculoRenta] Parámetros enviados a #actualizaRentasCliente");
                    getLogger().debug("[grabaCalculoRenta] RUT = {" + rutCliente + "}");
                    getLogger().debug("[grabaCalculoRenta] RUT DV = {" + dv + "}");
                    getLogger().debug("[grabaCalculoRenta] Renta = {" + evaluacionCliente.toString() + "}");
                }                

                boolean grabaCliente = actualizaRentasCliente(rutCliente, dv.charAt(0), evaluacionCliente);
                
                if (getLogger().isDebugEnabled()) {
                getLogger().debug("[grabaCalculoRenta] método #actualizaRentasCliente ejecutado correctamente");
                    getLogger().debug("[grabaCalculoRenta] Respuesta de método #actualizaRentasCliente, graba? = {"
                                     + grabaCliente + "}");
                }
                
                if (getLogger().isEnabledFor(Level.DEBUG) ){
                	getLogger().debug("[grabaCalculoRenta] Se llama a método #actualizaDatosHistoricos");
                    getLogger().debug("[grabaCalculoRenta] Parámetros enviados:");
                    getLogger().debug("[grabaCalculoRenta] RUT = {" + evaluacionCliente.getRutCliente() + "}");
                    getLogger().debug("[grabaCalculoRenta] DvRut = {" + evaluacionCliente.getDvRut() + "}");
                    getLogger().debug("[grabaCalculoRenta] Núm. Secuencia = {" + respuestaServicio + "}");
                    getLogger().debug("[grabaCalculoRenta] Indicador(1=falla;0=OK) = {0}");
                }

                actualizaDatosHistoricos(String.valueOf(evaluacionCliente.getRutCliente()),
                        String.valueOf(evaluacionCliente.getDvRut()), respuestaServicio, 0);
                if (getLogger().isEnabledFor(Level.DEBUG) ){
                    getLogger().debug("[grabaCalculoRenta] llamada a método "
                                      + "#actualizaDatosHistoricos ejecutada correctamente");
                }
            } 
            catch (TuxedoException e) {
                
                if (getLogger().isEnabledFor(Level.ERROR)) {
                    getLogger().error("[grabaCalculoRenta] Excepcion de TuxedoException: "+e.toString());
                    getLogger().error("[grabaCalculoRenta] Actualizando datos Tcorp, volviendo "
                                      + "a los valores anteriores");
                    getLogger().error("[grabaCalculoRenta] TuxedoExcepción, mensaje error: "
                                      + e.getSimpleMessage());
                    getLogger().error("[grabaCalculoRenta] TuxedoExcepción, código error: " + e.getCodigo());
                    getLogger().error("[grabaCalculoRenta] Llamando a método #actualizaDatosTcorp");
                    getLogger().error("[grabaCalculoRenta] Parámetros enviados a #actualizaDatosTcorp:");
                    getLogger().error("[grabaCalculoRenta] RUT = {" + evaluacionCliente.getRutCliente() + "}");
                    getLogger().error("[grabaCalculoRenta] GraboTcorp = {" + graboTcorp + "}");
                }
                actualizaDatosTcorp(String.valueOf(evaluacionCliente.getRutCliente()), graboTcorp);

                if (getLogger().isEnabledFor(Level.ERROR)) {
                    getLogger().error("[grabaCalculoRenta] Método #actualizaDatosTcorp ejecutado correctamente");
                    getLogger().error("[grabaCalculoRenta] Llamando a método #actualizaDatosHistoricos");
                    getLogger().error("[grabaCalculoRenta] Parámetros enviados a #actualizaDatosHistoricos:");
                    getLogger().error("[grabaCalculoRenta] RUT = {" + evaluacionCliente.getRutCliente() + "}");
                    getLogger().error("[grabaCalculoRenta] RUT DV = {" + evaluacionCliente.getDvRut() + "}");
                    getLogger().error("[grabaCalculoRenta] Núm. Secuencia = {" + respuestaServicio + "}");
                    getLogger().error("[grabaCalculoRenta] Indicador(1=falla;0=OK) = {1}");
                }
                actualizaDatosHistoricos(String.valueOf(evaluacionCliente.getRutCliente()),
                         String.valueOf(evaluacionCliente.getDvRut()), respuestaServicio, 1);
                if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[grabaCalculoRenta] Método #actualizaDatosHistoricos ejecutado correctamente");
                }
                if(getLogger().isEnabledFor(Level.ERROR)){
                    getLogger().error("[grabaCalculoRenta][BCI_FINEX][TuxedoException] error con mensaje: "
                                       + e.getMessage(), e);
                }
                throw e;
            } 
            catch (ClientesException e) {
                if (getLogger().isEnabledFor(Level.ERROR)) {
                    getLogger().error("[grabaCalculoRenta] Ha ocurrido un error al actualizar renta en SGC");
                    getLogger().error("[grabaCalculoRenta] Excepcion de ClienteException: "+e.toString());
                    getLogger().error("[grabaCalculoRenta] Actualizando datos Tcorp, "
                                       + "volviendo a los valores anteriores");
                    getLogger().error("[grabaCalculoRenta] ClientesExcepción, mensaje error: "
                                       + e.getSimpleMessage());
                    getLogger().error("[grabaCalculoRenta] ClientesExcepción, código error: "
                                       + e.getCodigo());
                    getLogger().error("[grabaCalculoRenta] Parámetros enviados a #actualizaDatosTcorp:");
                    getLogger().error("[grabaCalculoRenta] RUT = {" + evaluacionCliente.getRutCliente() + "}");
                    getLogger().error("[grabaCalculoRenta] GraboTcorp = {" + graboTcorp + "}");
                }

                actualizaDatosTcorp(String.valueOf(evaluacionCliente.getRutCliente()), graboTcorp);

                if (getLogger().isEnabledFor(Level.ERROR)) {
                    getLogger().error("[grabaCalculoRenta] Método #actualizaDatosTcorp ejecutado correctamente");
                    getLogger().error("[grabaCalculoRenta] Parámetros enviados a #actualizaDatosHistoricos:");
                    getLogger().error("[grabaCalculoRenta] RUT = {" + evaluacionCliente.getRutCliente() + "}");
                    getLogger().error("[grabaCalculoRenta] RUT DV = {" + evaluacionCliente.getDvRut() + "}");
                    getLogger().error("[grabaCalculoRenta] Núm. Secuencia = {" + respuestaServicio + "}");
                    getLogger().error("[grabaCalculoRenta] Indicador(1=falla;0=OK) = {1}");
                }                
                actualizaDatosHistoricos(String.valueOf(evaluacionCliente.getRutCliente()),
                        String.valueOf(evaluacionCliente.getDvRut()), respuestaServicio, 1);
                if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[grabaCalculoRenta] Método #actualizaDatosHistoricos ejecutado correctamente");
                }
                if(getLogger().isEnabledFor(Level.ERROR)){
                    getLogger().error("[grabaCalculoRenta][BCI_FINEX][ClientesException] error con mensaje: "
                                       + e.getMessage(), e);
                }                
                throw e;
                }                
            catch (GeneralException e) {
                if (getLogger().isEnabledFor(Level.ERROR)) {
                    getLogger().error("[grabaCalculoRenta] Ha ocurrido un error al actualizar renta en SGC");
                    getLogger().error("[grabaCalculoRenta] Excepcion de GeneralException: "+e.toString());
                    getLogger().error("[grabaCalculoRenta] Actualizando datos Tcorp, volviendo a "
                                      + "los valores anteriores");
                    getLogger().error("[grabaCalculoRenta] GeneralExcepción, mensaje error: " 
                                      + e.getSimpleMessage());
                    getLogger().error("[grabaCalculoRenta] GeneralExcepción, código error: " + e.getCodigo());
                    getLogger().error("[grabaCalculoRenta] Llamando a método #actualizaDatosTcorp");
                    getLogger().error("[grabaCalculoRenta] Parámetros enviados a #actualizaDatosTcorp:");
                    getLogger().error("[grabaCalculoRenta] RUT = {" + evaluacionCliente.getRutCliente() + "}");
                    getLogger().error("[grabaCalculoRenta] GraboTcorp = {" + graboTcorp + "}");
                }                
                actualizaDatosTcorp(String.valueOf(evaluacionCliente.getRutCliente()), graboTcorp);
                if (getLogger().isEnabledFor(Level.ERROR)) {
                    getLogger().error("[grabaCalculoRenta] Método #actualizaDatosTcorp ejecutado correctamente");
                    getLogger().error("[grabaCalculoRenta] Parámetros enviados a #actualizaDatosHistoricos:");
                    getLogger().error("[grabaCalculoRenta] RUT = {" + evaluacionCliente.getRutCliente() + "}");
                    getLogger().error("[grabaCalculoRenta] RUT DV = {" + evaluacionCliente.getDvRut()+ "}");
                    getLogger().error("[grabaCalculoRenta] Núm. Secuencia = {" + respuestaServicio + "}");
                    getLogger().error("[grabaCalculoRenta] Indicador(1=falla;0=OK) = {1}");
                }                
                actualizaDatosHistoricos(String.valueOf(evaluacionCliente.getRutCliente()),
                        String.valueOf(evaluacionCliente.getDvRut()), respuestaServicio, 1);
                if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[grabaCalculoRenta] Método #actualizaDatosHistoricos ejecutado correctamente");
                }
                
                if(getLogger().isEnabledFor(Level.ERROR)){
                    getLogger().error("[grabaCalculoRenta][BCI_FINEX][GeneralException] error con mensaje: "
                                       + e.getMessage(), e);
                }                
                throw e;
            }
        }
        /*  ACTUALIZACION SGC FIN */
        getLogger().debug("[grabaCalculoRenta] Fin ejecución");
        resultadoRegistroRenta.setResultado(true);
        
        if (getLogger().isEnabledFor(Level.INFO)){
            getLogger().info("[grabaCalculoRenta][BCI_FINOK] retornando resultadoRegistroRenta "
                + resultadoRegistroRenta);
        }
        return resultadoRegistroRenta;
    }


    /**
     * <p> Calcula renta cliente con liquidación de sueldo</p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>1.1  09/09/2008, Manuel Durán M. (Imagemaker IT): Se modifica interfaz, añadiendo y retornando el
     *          TO de cálculo de renta con la renta calculada, debido al cambio de stateful a stateless. Se
     *          elimina código comentado. Además se agrega javadoc.
     * <li>2.0  15/04/2014, Pedro Rebolledo Lagno (SEnTRA): Se agrega lógica para la definición del codigo de 
     *                                                      origen y el origen de calculo para la renta.
     * </ul>
     * 
     * @param rentaTO TO de cálculo de renta
     * @return el TO de CalculoRenta con datos calculados
     * @throws wcorp.serv.renta.CalculoRentaException
     * @throws wcorp.util.com.TuxedoException
     * @since ? 
     */
    public CalculoRentaTO calculoRentaConLiquidacion(CalculoRentaTO rentaTO) throws CalculoRentaException,
    TuxedoException {

        log.debug("bprocess.calculoRentaConLiquidacion()");

        LiquidacionDeSueldo[] liquidaciones = rentaTO.getEvaluacionCliente().getLiquidacionesDeSueldo();
        BoletaHonorarios[] boletas = rentaTO.getEvaluacionCliente().getBoletasHonorarios();

        int numeroBoletas = boletas != null ? boletas.length : 0;
        int len = liquidaciones == null ? 0 : liquidaciones.length;
        int choice = 0;

        if (log.isDebugEnabled()) {
            log.debug("Largo de liquidaciones : " + len);
        }

        double arriendoBienesRaices = rentaTO.getEvaluacionCliente().getArriendoBienesRaices();
        double pensionJubilacion = rentaTO.getEvaluacionCliente().getPensionJubilacion();        
        
        if (log.isDebugEnabled()) {
            log.debug("arriendoBienesRaices : " + arriendoBienesRaices);
            log.debug("pensionJubilacion : " + pensionJubilacion);
        }
        
        if(arriendoBienesRaices > 0){
            rentaTO.getEvaluacionCliente().setCodOrigen(EstadoCalculo.TYPE_ARRIENDO);
            rentaTO.getEvaluacionCliente().setOrigenCalculo(
                    EstadoCalculo.glosaEstado[EstadoCalculo.TYPE_ARRIENDO]);    
        }
        
        if(pensionJubilacion > 0){
            rentaTO.getEvaluacionCliente().setCodOrigen(EstadoCalculo.TYPE_PENSJUB);
            rentaTO.getEvaluacionCliente().setOrigenCalculo(
                    EstadoCalculo.glosaEstado[EstadoCalculo.TYPE_PENSJUB]);     
        }
        
        if(arriendoBienesRaices > 0 && pensionJubilacion > 0){
            rentaTO.getEvaluacionCliente().setCodOrigen(EstadoCalculo.TYPE_ARR_PENS);
        rentaTO.getEvaluacionCliente().setOrigenCalculo(
                    EstadoCalculo.glosaEstado[EstadoCalculo.TYPE_ARR_PENS]);
        }
        
        if(arriendoBienesRaices == 0 && pensionJubilacion == 0){
        rentaTO.getEvaluacionCliente().setCodOrigen(EstadoCalculo.TYPE_CALCULOLIQUIDACION);
        rentaTO.getEvaluacionCliente().setOrigenCalculo(
                EstadoCalculo.glosaEstado[EstadoCalculo.TYPE_CALCULOLIQUIDACION]);
        }
        
        if (log.isDebugEnabled()) {
            log.debug("CodOrigen     : " + rentaTO.getEvaluacionCliente().getCodOrigen());
            log.debug("OrigenCalculo : " + rentaTO.getEvaluacionCliente().getOrigenCalculo());
        }

        /*
         * Calculo de Total Ingresos Fijos
         */
        rentaTO.getEvaluacionCliente().setRentaFija(
                (liquidaciones[0].getPorcentajeIngresosFijos() * liquidaciones[0].getTotalRLMajustada()) / 1000);
        rentaTO.getEvaluacionCliente().setPeriodoLiq(
                liquidaciones[0].getPeriodoLiquidacion() + liquidaciones[len - 1].getPeriodoLiquidacion());
        /*
         * Modificacion de condicion segun requerimiento. Se exigen a lo menos 3
         * boletas para el calculo. 06-12-2004, Apolo Ingeniería
         */
        if (log.isDebugEnabled()) {
            log.debug("numeroBoletas : " + numeroBoletas);
        }
        if (numeroBoletas > 0) {
            rentaTO.getEvaluacionCliente().setPeriodoBol(
                    boletas[0].getPeriodoBoleta() + boletas[numeroBoletas - 1].getPeriodoBoleta());
        }
        float minLiq = 0;
        float maxLiq = 0;
        float minTot = 0;
        float maxTot = 0;
        float sum = 0;
        float sumSeisLiq = 0;
        int lenSeisLiq = 0;
        boolean esmenor = false;
        if (log.isDebugEnabled()) {
            log.debug("RVariable: [" + rentaTO.getParametrosCalculo().getRVariable() + "]");
        }

        minTot = liquidaciones[0].getTotalIngresosVariables();
        maxTot = liquidaciones[0].getTotalIngresosVariables();
        minLiq = liquidaciones[0].getPorcentajeIngresosVariables() * liquidaciones[0].getTotalRLMajustada();
        maxLiq = liquidaciones[0].getPorcentajeIngresosVariables() * liquidaciones[0].getTotalRLMajustada();

        for (int i = 0; i < len; i++) {
            if (log.isDebugEnabled()) {
                log.debug("LIQUIDACION[" + i + "]");
            }
            if (log.isDebugEnabled()) {
                log.debug("PorcentajeIngresosFijos: [" + liquidaciones[i].getPorcentajeIngresosFijos() + "]");
            }
            if (log.isDebugEnabled()) {
                log.debug("PorcentajeIngresosVariables: [" + liquidaciones[i].getPorcentajeIngresosVariables()
                        + "]");
            }
            if (log.isDebugEnabled()) {
                log.debug("TotalRLMajustada: [" + liquidaciones[i].getTotalRLMajustada() + "]");
            }
            if (log.isDebugEnabled()) {
                log.debug("minLiq [" + minLiq + "]");
            }
            if (log.isDebugEnabled()) {
                log.debug("maxLiq [" + maxLiq + "]");
            }
            if (log.isDebugEnabled()) {
                log.debug("minTot [" + minTot + "]");
            }
            if (log.isDebugEnabled()) {
                log.debug("maxTot [" + maxTot + "]");
            }
            if (log.isDebugEnabled()) {
                log.debug("liquidaciones[" + i + "].getTotalIngresosVariables()= "
                        + liquidaciones[i].getTotalIngresosVariables());
            }
            if (log.isDebugEnabled()) {
                log.debug("liquidaciones[" + i
                        + "].getPorcentajeIngresosVariables() * liquidaciones[i].getTotalRLMajustada() = "
                        + liquidaciones[i].getPorcentajeIngresosVariables()
                        * liquidaciones[i].getTotalRLMajustada());
            }

            if (liquidaciones[i].getPorcentajeIngresosFijos() < rentaTO.getParametrosCalculo().getRVariable()) {
                if (log.isDebugEnabled()) {
                    log.debug("ES MENOR TRUE: " + liquidaciones[i].getPorcentajeIngresosFijos() + " < "
                            + rentaTO.getParametrosCalculo().getRVariable());
                }
                esmenor = true;
            }

            if (liquidaciones[i].getTotalIngresosVariables() <= minTot) {
                minLiq = liquidaciones[i].getPorcentajeIngresosVariables()
                        * liquidaciones[i].getTotalRLMajustada();
                minTot = liquidaciones[i].getTotalIngresosVariables();
                if (log.isDebugEnabled()) {
                    log.debug("CAMBIA min, " + liquidaciones[i].getTotalIngresosVariables() + "<= " + minTot);
                }
                if (log.isDebugEnabled()) {
                    log.debug("CAMBIA min, " + liquidaciones[i].getPorcentajeIngresosVariables()
                            * liquidaciones[i].getTotalRLMajustada() + "<= " + minLiq);
                }
            }

            if (liquidaciones[i].getTotalIngresosVariables() > maxTot) {
                maxLiq = liquidaciones[i].getPorcentajeIngresosVariables()
                        * liquidaciones[i].getTotalRLMajustada();
                maxTot = liquidaciones[i].getTotalIngresosVariables();
                if (log.isDebugEnabled()) {
                    log.debug("CAMBIA max, " + liquidaciones[i].getTotalIngresosVariables() + "> " + maxTot);
                }
                if (log.isDebugEnabled()) {
                    log.debug("CAMBIA max, " + liquidaciones[i].getPorcentajeIngresosVariables()
                            * liquidaciones[i].getTotalRLMajustada() + "> " + maxLiq);
                }
            }

            sum = sum + liquidaciones[i].getPorcentajeIngresosVariables() * liquidaciones[i].getTotalRLMajustada();
        }

        if (log.isDebugEnabled()) {
            log.debug("MIN      [" + minLiq + "]");
        }
        if (log.isDebugEnabled()) {
            log.debug("MAX      [" + maxLiq + "]");
        }
        if (log.isDebugEnabled()) {
            log.debug("MIN      [" + minTot + "]");
        }
        if (log.isDebugEnabled()) {
            log.debug("MAX      [" + maxTot + "]");
        }
        if (log.isDebugEnabled()) {
            log.debug("SUM         [" + sum + "]");
        }
        if (log.isDebugEnabled()) {
            log.debug("LEN         [" + len + "]");
        }

        if (len >= 6) {
            // 6 liquidaciones
            if (log.isDebugEnabled()) {
                log.debug("Monto Mayor: [" + maxLiq + "]");
            }
            if (log.isDebugEnabled()) {
                log.debug("Monto menor: [" + minLiq + "]");
            }
            if (log.isDebugEnabled()) {
                log.debug("SUM con extremos        [" + sum + "]");
            }
            sumSeisLiq = sum - (maxLiq + minLiq);
            lenSeisLiq = len - 2;
            if (log.isDebugEnabled()) {
                log.debug("SUM sin extremos        [" + sumSeisLiq + "]");
            }
            if (log.isDebugEnabled()) {
                log.debug("OJO BH Actualizada: " + rentaTO.getEvaluacionCliente().getBhActualizada());
            }

            // Elias 09122004
            float fRentaVariable = (sumSeisLiq / lenSeisLiq) * rentaTO.getParametrosCalculo().getPonderadorRenta();
            if (numeroBoletas >= 3)
                fRentaVariable += rentaTO.getEvaluacionCliente().getBhActualizada() * 1000;
            // ---

            if (log.isDebugEnabled()) {
                log.debug("(sumSeisLiq / lenSeisLiq)* parametrosCalculo.getPonderadorRenta(): "
                        + (sumSeisLiq / lenSeisLiq) * rentaTO.getParametrosCalculo().getPonderadorRenta());
            }
            if (log.isDebugEnabled()) {
                log.debug("fRentaVariable: " + fRentaVariable);
            }
            rentaTO.getEvaluacionCliente().setRentaVariable(fRentaVariable);
        } else {

            // Elias 09122004
            if (numeroBoletas >= 3) {
                if (log.isDebugEnabled()) {
                    log.debug("NO ES MENOR SUM: " + sum + ", len: " + len + ", sum/len: " + (sum / len) + ", BH"
                            + rentaTO.getEvaluacionCliente().getBhActualizada());
                }
                rentaTO.getEvaluacionCliente().setRentaVariable(
                        (sum / len) + rentaTO.getEvaluacionCliente().getBhActualizada() * 1000);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("NO ES MENOR SUM: " + sum + ", len: " + len + ", sum/len: " + (sum / len));
                }
                rentaTO.getEvaluacionCliente().setRentaVariable((sum / len));
            }
            // ----
        }

        if (log.isDebugEnabled()) {
            log.debug("RENTA FIJA       [" + rentaTO.getEvaluacionCliente().getRentaFija() + "]");
        }
        if (log.isDebugEnabled()) {
            log.debug("RENTA VARIABLE   [" + rentaTO.getEvaluacionCliente().getRentaVariable() + "]");
        }

        return rentaTO;
    }

    /**
     * <p> Calcula renta cliente con Declaracion Anual de Impuestos</p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>1.1  09/09/2008, Manuel Durán M. (Imagemaker IT): Se modifica interfaz, añadiendo y retornando el
     *          TO de cálculo de renta con la renta calculada, debido al cambio de stateful a stateless. Para el
     *          caso de no existir DAI se retorna TO en vez de null para no afectar los demás atributos
     *          del TO. Se elimina código comentado. Además se agrega javadoc.
     * </ul>
     * 
     * @param rentaTO TO de cálculo de renta
     * @return el TO de CalculoRenta con datos calculados
     * @throws wcorp.serv.renta.CalculoRentaException
     * @since ? 
     */
    public CalculoRentaTO calculoRentaConDai(CalculoRentaTO rentaTO) throws CalculoRentaException {

        log.debug("bprocess.calculoRentaConDai()");
        if (rentaTO.getEvaluacionCliente() != null) {
            log.debug("EvaluacionCliente es igual a null");
        } else {
            log.debug("EvaluacionCliente es null");
        }

        BoletaHonorarios[] boletas = rentaTO.getEvaluacionCliente().getBoletasHonorarios();
        int numeroBoletas = boletas != null ? boletas.length : 0;
        /*
         * Modificacion de condicion segun requerimiento. Se exigen a lo menos 3
         * boletas para el calculo. 06-12-2004, Apolo Ingeniería
         */
        if (numeroBoletas > 0) {
            rentaTO.getEvaluacionCliente().setPeriodoBol(
                    boletas[0].getPeriodoBoleta() + boletas[numeroBoletas - 1].getPeriodoBoleta());
        }

        Dai dai = rentaTO.getEvaluacionCliente().getDai();

        if (dai == null) {
            log.debug("Error Inesperado : No existe informacion para el DAI");
            return rentaTO;
        }

        rentaTO.getEvaluacionCliente().setCodOrigen(EstadoCalculo.TYPE_CALCULODAI);
        rentaTO.getEvaluacionCliente().setOrigenCalculo(EstadoCalculo.glosaEstado[EstadoCalculo.TYPE_CALCULODAI]);

        /*
         * Calculo de Total Ingresos Fijos
         */
        log.debug("Calculando Ingresos Por Dai");
        CalculaIngresosPorDai(rentaTO.getEvaluacionCliente().getDai(), rentaTO);
        /*
         * Modificacion de condicion segun requerimiento. Se exigen a lo menos 3
         * boletas para el calculo. 06-12-2004, Apolo Ingeniería
         */
        if (numeroBoletas >= 3) {
            log.debug("Calculando Ingresos Por Boletas");
            rentaTO = CalculaIngresosPorBoletas(rentaTO);
            log.debug("Calculando Dai Con Boletas");
            calculoDaiConBoletas(rentaTO.getEvaluacionCliente().getDai(), rentaTO);

            rentaTO.getEvaluacionCliente().setRentaFija(dai.getTotalIngresosMensualesPorE33());
            rentaTO.getEvaluacionCliente().setRentaVariable(dai.getTotalIngresosVariablesPorE34());
        } else {
            log.debug("No incluir calculo por Boletas");
            if (log.isDebugEnabled()) {
                log.debug("El total de ingresos por arriendos: "
                        + rentaTO.getEvaluacionCliente().getArriendoBienesRaices());
            }
            if (log.isDebugEnabled()) {
                log.debug("El total de ingresos por pension jubilacion: "
                        + rentaTO.getEvaluacionCliente().getPensionJubilacion());
            }
            if (log.isDebugEnabled()) {
                log.debug("El total de ingresos por pension alimenticia: "
                        + rentaTO.getEvaluacionCliente().getPensionAlimenticia());
            }
            rentaTO.getEvaluacionCliente().setRentaFija(dai.getTotalIngresosFijosSGC());
            rentaTO.getEvaluacionCliente().setRentaVariable(dai.getTotalIngresosVariableSGC());
            if (log.isDebugEnabled()) {
                log.debug("TotalIngresosFijosSGC: " + dai.getTotalIngresosFijosSGC());
            }
            if (log.isDebugEnabled()) {
                log.debug("TotalIngresosVariablesSGC: " + dai.getTotalIngresosVariableSGC());
            }
        }
        rentaTO.getEvaluacionCliente().setPeriodoDai(dai.getPeriodoDai());

        if (log.isDebugEnabled()) {
            log.debug("xRENTA VARIABLE CON POL      [" + dai.getTotalIngresosVariablesPorE34() + "]");
        }
        if (log.isDebugEnabled()) {
            log.debug("xRENTA FIJA      [" + rentaTO.getEvaluacionCliente().getRentaFija() + "]");
        }
        if (log.isDebugEnabled()) {
            log.debug("xRENTA VARIABLE  [" + rentaTO.getEvaluacionCliente().getRentaVariable() + "]");
        }

        return rentaTO;
    }
    /**
     * Método que obtiene el cálculo historico del cliente
     * <p>
     * @param esClienteBci indica si ya es cliente bci
     * @param rutCliente rut del cliente del cual se quiere obtener sus cálculos historicos
     * @param dvRut digito verificador del cliente
     * @param ejecutivo ejecutivo que solicita el cálculo
     * @param rentaTO TO de cálculo de renta
     * @return Calculo históricos del cliente
     * <p>
     * Registro de versiones:<ul>
     * <li>1.0 sin fecha Incorporacion - sin autor - versión inicial
     * <li>1.1 10-11-2005 - Emilio Fuentealba Silva (SEnTRA)- se incluye la
     * obtención de los ingresos adicionles en la llamada del metodo privado
     * "cargaIngresosAdicionales"
     * <li>1.2 09/09/2008, Manuel Durán M. (Imagemaker IT): Se modifica interfaz, añadiendo el 
     *         TO de cálculo de renta, debido al cambio de stateful a stateless. </li>
     * </ul>
     * <p>
     *  
     * @throws wcorp.serv.renta.CalculoRentaException
     * @since 1.0
     */

    public RentaCliente[] obtieneCalculoHistoricoCliente(boolean esClienteBci, String rutCliente, String dvRut,
            String ejecutivo, CalculoRentaTO rentaTO) throws CalculoRentaException {
        RentaCliente calculoCliente = null;

        log.debug("*******************************************");
        log.debug("bprocess.CalculoRenta.obtieneCalculoHistoricoCliente");
        log.debug("*******************************************\n\n\n\n");
        if (log.isDebugEnabled()) {
            log.debug("ES CLIENTE : " + esClienteBci);
        }

        /**
         * Consultamos el Historico y seteamos los datos de la Renta.
         */
        try {
            CalculoHistorico historico[] = trxBean.calculoHistorico(rutCliente, dvRut);
            int num_historicos = historico != null ? historico.length : 0;
            RentaCliente rentas[] = new RentaCliente[num_historicos];
            if (log.isDebugEnabled()) {
                log.debug("PROCESANDO: " + num_historicos + " RENTAS HISTORICAS");
            }
            for (int h = 0; h < num_historicos; h++) {
                rentas[h] = new RentaCliente();
                rentas[h].setRutCliente(rutCliente);
                rentas[h].setDvRut(dvRut);

                rentas[h].setOrigenCalculo(historico[h].getOrigen());
                rentas[h].setConyugeTrabaja(historico[h].getTrabajaConyuge());
                rentas[h].setEsClienteBci(esClienteBci);
                rentas[h].setEstadoCivil(historico[h].getHombreSeparado());
                rentas[h].setFechaActualiza(historico[h].getFechaModificacion());
                rentas[h].setFechIngreso(historico[h].getFechaIngreso());
                rentas[h].setPerteneceFFAA(historico[h].getFuerzasArmada());
                rentas[h].setGradoFFAA(historico[h].getGrado());
                rentas[h].setNombreEmpleador("");
                rentas[h].setRutEmpleador("");
                rentas[h].setUsuarioActualiza(historico[h].getUsuarioModificacion());
                rentas[h].setPeriodoBol(historico[h].getPeriodoBoleta());
                rentas[h].setPeriodoDai(historico[h].getPeriodoDai());
                rentas[h].setPeriodoLiq(historico[h].getPeriodoLiquidacion());
                rentas[h].setUsuarioIngreso(historico[h].getEjecutivoIngreso());
                rentas[h].setNumeroSecuencia(historico[h].getSecuencia());
                rentas[h].setNumHijos(historico[h].getNumeroHijos());
                rentas[h].setOrigenCalculo(historico[h].getOrigen());
                rentas[h].setLiquidacion(historico[h].isLiquidacion());
                rentas[h].setDai(historico[h].isDai());
                rentas[h].setRentaFija(historico[h].getRentaLiquida());
                rentas[h].setRentaVariable(historico[h].getRentaVariable());
                /**
                 * Obtenemos las liquidaciones de sueldo con el numerosecuencia
                 * y el primero periodo correspondiente.
                 */
                if (rentas[h].isLiquidacion()) {
                    if (log.isDebugEnabled()) {
                        log.debug("CARGANDO LIQUIDACIONES DE SUELDO PARA RENTA[" + h + "] CON SECUENCIA: "
                                + rentas[h].getNumeroSecuencia() + ", PERIODO DAI=" + rentas[h].getPeriodoDai()
                                + ", PERIODO LIQUIDACION=" + rentas[h].getPeriodoLiq());
                    }
                    cargaLiquidacionesDeSueldo(rentas[h], true, rentaTO);

                    /**
                     * Cargamos las boletas de honorarios.
                     */
                    if (log.isDebugEnabled()) {
                        log.debug("CARGANDO BOLETAS DE HONORARIOS PARA RENTA[" + h + "] CON SECUENCIA: "
                                + rentas[h].getNumeroSecuencia() + ", PERIODO DAI=" + rentas[h].getPeriodoDai()
                                + ", PERIODO LIQUIDACION=" + rentas[h].getPeriodoLiq());
                    }
                    cargaBoletaHonorarios(rentas[h], true, rentaTO);
                    /**
                     * Carga los ingresos adicionales
                     */
                    if (log.isDebugEnabled()) {
                        log.debug("CARGANDO INGRESOS ADICIONALES[" + h + "] CON SECUENCIA: "
                                + rentas[h].getNumeroSecuencia() + ", PERIODO DAI=" + rentas[h].getPeriodoDai()
                                + ", PERIODO LIQUIDACION=" + rentas[h].getPeriodoLiq());
                    }
                    cargaIngresosAdicionales(rentas[h], true);

                }

                /**
                 * Cargamos los detalles DAI
                 */
                if (rentas[h].isDai()) {
                    if (log.isDebugEnabled()) {
                        log.debug("CARGANDO DAI PARA RENTA[" + h + "] CON SECUENCIA: "
                                + rentas[h].getNumeroSecuencia() + ", PERIODO DAI=" + rentas[h].getPeriodoDai()
                                + ", PERIODO LIQUIDACION=" + rentas[h].getPeriodoLiq());
                    }
                    cargaDai(rentas[h], true, rentaTO);

                    /**
                     * Cargamos las boletas de honorarios.
                     */
                    if (log.isDebugEnabled()) {
                        log.debug("CARGANDO BOLETAS DE HONORARIOS PARA RENTA[" + h + "] CON SECUENCIA: "
                                + rentas[h].getNumeroSecuencia() + ", PERIODO DAI=" + rentas[h].getPeriodoDai()
                                + ", PERIODO LIQUIDACION=" + rentas[h].getPeriodoLiq());
                    }
                    cargaBoletaHonorarios(rentas[h], true, rentaTO);
                    /**
                     * Carga los ingresos adicionales
                     */
                    if (log.isDebugEnabled()) {
                        log.debug("CARGANDO INGRESOS ADICIONALES[" + h + "] CON SECUENCIA: "
                                + rentas[h].getNumeroSecuencia() + ", PERIODO DAI=" + rentas[h].getPeriodoDai()
                                + ", PERIODO LIQUIDACION=" + rentas[h].getPeriodoLiq());
                    }
                    cargaIngresosAdicionales(rentas[h], true);

                }
            }

            if (num_historicos > 0) {
                return rentas;
            }

        } catch (Exception e) {
            if( log.isEnabledFor(Level.ERROR) ){
                log.error("[obtieneCalculoHistoricoCliente] Ha ocurrido un error: " + e);
            }
        }
        return null;
    }

    /**
     * <p> Modifica Ejecutivo que ingresa y actualiza renta</p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>1.1  09/09/2008, Manuel Durán M. (Imagemaker IT): Se depreca ya que no es utilizado y el 
     *          seteo de ejecutivo que ingresa/actualiza se realiza directamente a nivel de servlets</li>
     * </ul>
     * 
     * @since ?
     * @throws wcorp.serv.renta.CalculoRentaException
     * @deprecated La modificación del ejecutivo que actualiza/ingresa se realiza a nivel de servlets.
     */
    public boolean modificaEjecutivo(String ejAct, String ejeIng) throws CalculoRentaException {

        return false;
    }

    /**
     * Consulta cálculo de ingresos registrados
     * <p>
     * Registro de versiones:<ul>
     * <li> 1.0 ??/??/???? desconocido - version inicial
     *
     * <li> 2.0 06/04/2006 Nelly Reyes Quezada, SEnTRA - Se agrega log para la detección de errores
     * 
     * <li> 2.1 09/09/2008, Manuel Durán M. (Imagemaker IT): Se modifica interfaz, añadiendo y retornando el
     *          TO de cálculo de renta con la renta calculada, debido al cambio de stateful a stateless. Además
     *          para las llamadas a carga de liquidaciones, DAI, boletas e ingresos adicionales se agrega el TO
     *          de cálculo de renta.Se elimina código comentado.</li>
     * <li> 2.2 12/09/2008, Manuel Durán M. (Imagemajer IT): Se Agrega modifica condición para creación de 
     *          nuevo cálculo, aparte de no estar pendiente debe estár dentro de la fecha de permanencia
     *          entregada por  {@link #obtieneFechaPermanencia()}.
     * <li> 3.0 15/04/2014 Pedro Rebolledo Lagno (SEnTRA): 
     *                          Se agrega lógica para diferenciar por el origen del calculo.
     *                          Se ajustan las líneas de logueo según la Normativa Vigente.
     *                                                     
     * </ul>
     * <p>
     * @param  esClienteBci indica si es cliente BCI
     * @param  rutCliente rut del cliente
     * @param  dvRut dígito verificador del cliente
     * @param  ejecutivo que consulta
     * @param  reglaDe15dias para el cálculo del periodo
     * @param  rentaTO TO de cálculo de renta
     * @return el TO de CalculoRenta con datos calculados
     * @throws wcorp.serv.renta.CalculoRentaException
     * @since 3.0
     */

    public CalculoRentaTO obtieneCalculoCliente(boolean esClienteBci, String rutCliente, String dvRut,
            String ejecutivo, boolean reglaDe15dias, CalculoRentaTO rentaTO) throws CalculoRentaException {

        RentaCliente calculoCliente = null;

        getLogger().debug("*******************************************");
        getLogger().debug("bprocess.CalculoRenta.obtieneCalculoCliente");
        getLogger().debug("*******************************************\n\n\n\n");
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("ES CLIENTE : " + esClienteBci);
        }

        if ((!rentaTO.isPeriodosCargados()) && (rentaTO.getEvaluacionCliente() != null))
            rentaTO = obtieneInformacionDelPeriodo(rutCliente, dvRut, reglaDe15dias, rentaTO);

        if (rentaTO.getEvaluacionCliente() != null) {
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("CLIENTE: " + rentaTO.getEvaluacionCliente().getRutCliente());
            }
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("CLIENTE Renta Fija: " + rentaTO.getEvaluacionCliente().getRentaFija());
            }
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("CLIENTE Renta Variable: " + rentaTO.getEvaluacionCliente().getRentaVariable());
            }
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("EvaluacionCliente.getArriendoBienesRaices: "
                        + rentaTO.getEvaluacionCliente().getArriendoBienesRaices());
            }
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("EvaluacionCliente.getPensionJubilacion: "
                        + rentaTO.getEvaluacionCliente().getPensionJubilacion());
            }
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("EvaluacionCliente.getPensionAlimenticia: "
                        + rentaTO.getEvaluacionCliente().getPensionAlimenticia());
            }
            if (rentaTO.getEvaluacionCliente().getOrigenCalculo().trim().compareToIgnoreCase(
                    EstadoCalculo.glosaEstado[EstadoCalculo.TYPE_PENDIENTECALCULO]) == 0)
                return rentaTO;
        }
        try {
            calculoCliente = trxBean.consultaUltimoCalculo(rutCliente, dvRut);
            if (calculoCliente == null) {
                getLogger().debug("CREA NUEVO CALCULO ");
                calculoCliente = new RentaCliente();
                calculoCliente.setEsClienteBci(esClienteBci);
                calculoCliente.setRutCliente(rutCliente);
                calculoCliente.setDvRut(dvRut);
                calculoCliente.setNumeroSecuencia(-1);
                calculoCliente.setUsuarioActualiza(ejecutivo);
                calculoCliente.setUsuarioIngreso(ejecutivo);
                calculoCliente.setOrigenCalculo(EstadoCalculo.glosaEstado[EstadoCalculo.TYPE_PENDIENTECALCULO]);
                rentaTO.setEvaluacionCliente(calculoCliente);
                return rentaTO;
            } else {
                Date fechaActualizaRenta = calculoCliente.getFechaActualiza();
                Date fechaPermanecia = obtieneFechaPermanencia();
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("fechaActualizaRenta : " + fechaActualizaRenta);
                }
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("fechaPermanencia : " + fechaPermanecia);
                }                
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("rut: " + calculoCliente.getRutCliente() + " CLIENTE Renta Fija consulta: "
                            + calculoCliente.getRentaFija());
                }
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("rut: "+calculoCliente.getRutCliente()+" CLIENTE Renta Variable consulta: "
                            + calculoCliente.getRentaVariable());
                }
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("calculoCliente.getArriendoBienesRaices: "
                            + calculoCliente.getArriendoBienesRaices());
                }
                if (getLogger().isDebugEnabled()) {
                  getLogger().debug("calculoCliente.getPensionJubilacion: "+calculoCliente.getPensionJubilacion());
                }
                if (getLogger().isDebugEnabled()) {
                 getLogger().debug("calculoCliente.getPensionAlimenticia:"+calculoCliente.getPensionAlimenticia());
                }
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("Origen del Cálculo:" + calculoCliente.getOrigenCalculo());
                }
                getLogger().debug("UN CALCULO EXISTE....SE CARGA");
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("Usuario Actualiza: " + calculoCliente.getUsuarioActualiza());
                }
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("Usuario Ingreso: " + calculoCliente.getUsuarioIngreso());
                }
                if (!calculoCliente.getOrigenCalculo().equalsIgnoreCase(
                        EstadoCalculo.glosaEstado[EstadoCalculo.TYPE_PENDIENTECALCULO]) ||
                        fechaActualizaRenta.before(fechaPermanecia) 
                        ) {
                    calculoCliente = new RentaCliente();
                    calculoCliente.setEsClienteBci(esClienteBci);
                    calculoCliente.setRutCliente(rutCliente);
                    calculoCliente.setDvRut(dvRut);
                    calculoCliente.setNumeroSecuencia(-1);
                    calculoCliente.setUsuarioActualiza(ejecutivo);
                    calculoCliente.setUsuarioIngreso(ejecutivo);
                    calculoCliente.setCodOrigen(EstadoCalculo.TYPE_PENDIENTECALCULO);
                    calculoCliente
                    .setOrigenCalculo(EstadoCalculo.glosaEstado[EstadoCalculo.TYPE_PENDIENTECALCULO]);
                    getLogger().debug("CREA NUEVO CALCULO POR QUE NO ESTA PENDIENTE");
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("Usuario Actualiza: " + calculoCliente.getUsuarioActualiza());
                    }
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("Usuario Ingreso: " + calculoCliente.getUsuarioIngreso());
                    }
                    rentaTO.setEvaluacionCliente(calculoCliente);
                    return rentaTO;
                }

                calculoCliente.setUsuarioActualiza(ejecutivo);
                rentaTO.setEvaluacionCliente(calculoCliente);
                if (calculoCliente.getOrigenCalculo().trim().compareToIgnoreCase(
                        EstadoCalculo.glosaEstado[EstadoCalculo.TYPE_CALCULOLIQUIDACION]) == 0) {
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("CALCULO POR LIQUIDACION " + calculoCliente.getOrigenCalculo().trim());
                    }
                    calculoCliente.setCodOrigen(EstadoCalculo.TYPE_PENDIENTECALCULO);
                    calculoCliente
                    .setOrigenCalculo(EstadoCalculo.glosaEstado[EstadoCalculo.TYPE_PENDIENTECALCULO]);
                    calculoCliente.setNumeroSecuencia(-1);
                }
                else if (calculoCliente.getOrigenCalculo().trim().compareToIgnoreCase(
                        EstadoCalculo.glosaEstado[EstadoCalculo.TYPE_ARRIENDO]) == 0) {
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("CALCULO POR ARRIENDOS " + calculoCliente.getOrigenCalculo().trim());
                    }
                    calculoCliente.setCodOrigen(EstadoCalculo.TYPE_PENDIENTECALCULO);
                    calculoCliente
                            .setOrigenCalculo(EstadoCalculo.glosaEstado[EstadoCalculo.TYPE_PENDIENTECALCULO]);
                    calculoCliente.setNumeroSecuencia(-1);
                }
                else if (calculoCliente.getOrigenCalculo().trim().compareToIgnoreCase(
                        EstadoCalculo.glosaEstado[EstadoCalculo.TYPE_PENSJUB]) == 0) {
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("CALCULO POR PENSIONES/JUBILACION "
                                           + calculoCliente.getOrigenCalculo().trim());
                    }
                    calculoCliente.setCodOrigen(EstadoCalculo.TYPE_PENDIENTECALCULO);
                    calculoCliente
                            .setOrigenCalculo(EstadoCalculo.glosaEstado[EstadoCalculo.TYPE_PENDIENTECALCULO]);
                    calculoCliente.setNumeroSecuencia(-1);
                }
                else if (calculoCliente.getOrigenCalculo().trim().compareToIgnoreCase(
                        EstadoCalculo.glosaEstado[EstadoCalculo.TYPE_ARR_PENS]) == 0) {
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("CALCULO POR ARRIENDOS Y PENSIONES/JUBILACION " 
                                + calculoCliente.getOrigenCalculo().trim());
                    }
                    calculoCliente.setCodOrigen(EstadoCalculo.TYPE_PENDIENTECALCULO);
                    calculoCliente
                            .setOrigenCalculo(EstadoCalculo.glosaEstado[EstadoCalculo.TYPE_PENDIENTECALCULO]);
                    calculoCliente.setNumeroSecuencia(-1);
                }
                else if (calculoCliente.getOrigenCalculo().trim().compareToIgnoreCase(
                        EstadoCalculo.glosaEstado[EstadoCalculo.TYPE_CALCULODAI]) == 0) {
                    calculoCliente.setCodOrigen(EstadoCalculo.TYPE_PENDIENTECALCULO);
                    calculoCliente
                    .setOrigenCalculo(EstadoCalculo.glosaEstado[EstadoCalculo.TYPE_PENDIENTECALCULO]);
                    calculoCliente.setNumeroSecuencia(-1);
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("CALCULO POR DAI " + calculoCliente.getOrigenCalculo().trim());
                    }
                } else {
                    calculoCliente.setCodOrigen(EstadoCalculo.TYPE_PENDIENTECALCULO);
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("CARGO LAS LIQUIDACIONES DE SUELDO PERIODO : " 
                                + calculoCliente.getPeriodoLiq() + "*****************************");
                    }
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("Rut cargaLiquidacionesDeSueldo: " + calculoCliente.getRutCliente());
                    }
                    rentaTO.setEvaluacionCliente(calculoCliente);
                    cargaLiquidacionesDeSueldo(rentaTO);
                    cargaDai(rentaTO);
                    cargaBoletaHonorarios(rentaTO);
                    cargaIngresosAdicionales(rentaTO);
                }
                if (rentaTO.getEvaluacionCliente() != null) {
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("rut: " + rentaTO.getEvaluacionCliente().getRutCliente()
                                + "CLIENTE Renta Fija: " + rentaTO.getEvaluacionCliente().getRentaFija());
                    }
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("CLIENTE Renta Variable: " 
                                + rentaTO.getEvaluacionCliente().getRentaVariable());
                    }
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("EvaluacionCliente.getArriendoBienesRaices: "
                                + rentaTO.getEvaluacionCliente().getArriendoBienesRaices());
                    }
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("EvaluacionCliente.getPensionJubilacion: "
                                + rentaTO.getEvaluacionCliente().getPensionJubilacion());
                    }
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("EvaluacionCliente.getPensionAlimenticia: "
                                + rentaTO.getEvaluacionCliente().getPensionAlimenticia());
                    }
                }
                return rentaTO;
            }
        } catch (Exception ex) {
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("Problemas (bprocess.CalculoRenta.consultaUltimoCalculo) " + ex.getMessage());
            }
            throw new CalculoRentaException(ex.getMessage());
        }
    }
    /**
     * Método que setea los datos del cliente en el VO RentaCliente
     * <p>
     * @param rCliente renta del cliente sobre la cual se desea realizar los calculos
     * <p>
     * Registro de versiones:<ul>
     * <li>1.0 sin fecha Incorporacion - sin autor - versión inicial
     * <li>1.1 10-11-2005 - Emilio Fuentealba Silva (SEnTRA)- se agrega el
     * dato del canal por donde se realizan las modificaciones
     * <li>1.2 09/09/2008, Manuel Durán M. (Imagemaker IT): Se depreca debido a cambios de stateful a 
     *         stateless, ya que se deberán setear sobre el TO desde servlet de donde es llamado .</li>
     * </ul>
     * <p>
     * @since 1.0
     * @deprecated Datos del cliente se deberán setear desde servlets, ya que se eliminó VO del bean 
     * debido a cambios de Stateful a Stateless
     */

    public RentaCliente setDatosCliente(RentaCliente rCliente) {
        return null;
    }

    /*
     * **********************************
     * SERVICIOS DE LIQUIDACION DE SUELDO
     * **********************************
     */

    /**
     * <p> Obtiene las liquidaciones de sueldo del periodo antes del 15</p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>1.1  09/09/2008, Manuel Durán M. (Imagemaker IT): Se elimina el seteo del empleador y del atributo 
     *          de clase ya que se realizaran sobre el TO desde el método que lo invoca, debido al cambio de 
     *          stateful a stateless. Además se agrega javadoc.</li>
     * </ul>
     * 
     * @param rutCliente rut del cliente
     * @param dvRut dígito verificador del cliente
     * @return las liquidaciones de sueldo del periodo encontradas
     * @throws wcorp.serv.renta.CalculoRentaException
     * @throws wcorp.util.com.TuxedoException
     * @throws java.rmi.RemoteException
     * @since ? 
     */ 
    private LiquidacionDeSueldo[] obtieneLiquidacionesDelPeriodoAntes15(String rutCliente, String dvRut)
            throws CalculoRentaException, TuxedoException, RemoteException {

        log.debug("CalculoRenta.obtieneLiquidacionesDelPeriodoAntes15");
        log.debug("*******************************************");

        int numeroSecuencia = -1;

        // CARGO TODAS LAS LIQUIDACIONES DE SUELDO
        String periodoLiquidacion = getRangoPeriodo(-7);
        if (log.isDebugEnabled()) {
            log.debug("El rango del periodo Liquidaciones es : " + periodoLiquidacion);
        }
        if (log.isDebugEnabled()) {
            log.debug("El rut cliente es: " + rutCliente + "-" + dvRut);
        }
        LiquidacionDeSueldo[] liquidaciones = this.trxBean.obtieneLiquidacionesDeSueldo(rutCliente, dvRut, -1,
                periodoLiquidacion);

        int len = liquidaciones == null ? 0 : liquidaciones.length;

        for (int i = 0; i < len; i++) {
            DetalleItem[] detalle = this.trxBean.obtieneDetalleLiquidacionesDeSueldo(rutCliente, dvRut,
                    liquidaciones[i].getNumeroSecuencia(), liquidaciones[i].getPeriodoLiquidacion()
                    + liquidaciones[i].getPeriodoLiquidacion());
            liquidaciones[i].setDetalleLiquidacion(detalle);
            liquidaciones[i].setGlosaLiquidacion(getGlosaPeriodo(liquidaciones[i].getPeriodoLiquidacion()));
        }

        return liquidaciones;

    }

    /**
     * <p> Obtiene las liquidaciones de sueldo del periodo despues del 15</p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>1.1  09/09/2008, Manuel Durán M. (Imagemaker IT): Se elimina el seteo del empleador y del atributo de
     *          clase ya que se realizaran sobre el TO desde el metodo que lo invoca, debido al cambio de 
     *          stateful a stateless. Además se agrega javadoc.</li>
     * </ul>
     * 
     * @param rutCliente rut del cliente
     * @param dvRut dígito verificador del cliente
     * @return las liquidaciones de sueldo del periodo encontradas
     * @throws wcorp.serv.renta.CalculoRentaException
     * @throws wcorp.util.com.TuxedoException
     * @throws java.rmi.RemoteException
     * @since ? 
     */ 
    private LiquidacionDeSueldo[] obtieneLiquidacionesDelPeriodoDespues15(String rutCliente, String dvRut)
            throws CalculoRentaException, TuxedoException, RemoteException {

        log.debug("CalculoRenta.obtieneLiquidacionesDelPeriodo");
        log.debug("*******************************************");

        int numeroSecuencia = -1;

        // CARGO TODAS LAS LIQUIDACIONES DE SUELDO
        String periodoLiquidacion = getRangoPeriodoDespues15(-6);
        if (log.isDebugEnabled()) {
            log.debug("El rango del periodo Liquidaciones es : " + periodoLiquidacion);
        }
        if (log.isDebugEnabled()) {
            log.debug("El rut cliente es: " + rutCliente + "-" + dvRut);
        }
        LiquidacionDeSueldo[] liquidaciones = this.trxBean.obtieneLiquidacionesDeSueldo(rutCliente, dvRut, -1,
                periodoLiquidacion);

        int len = liquidaciones == null ? 0 : liquidaciones.length;

        if (log.isDebugEnabled()) {
            log.debug("liquidaciones[0]        :" + liquidaciones[0]);
        }

        for (int i = 0; i < len; i++) {
            DetalleItem[] detalle = this.trxBean.obtieneDetalleLiquidacionesDeSueldo(rutCliente, dvRut,
                    liquidaciones[i].getNumeroSecuencia(), liquidaciones[i].getPeriodoLiquidacion()
                    + liquidaciones[i].getPeriodoLiquidacion());
            liquidaciones[i].setDetalleLiquidacion(detalle);
            liquidaciones[i].setGlosaLiquidacion(getGlosaPeriodo(liquidaciones[i].getPeriodoLiquidacion()));
        }

        return liquidaciones;

    }


    /**
     * <p> Carga las liquidaciones de sueldo de un periodo, permitiendo obtener las últimas 6 para el caso de 
     *     un cálculo de ingreso con más de un periodo o todas las liquidaciones para el caso de consulta de 
     *     historicos. Este método modifica los atributos del objeto de tipo RentaCliente </p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>1.1  26/06/2007, Manuel Durán M. (Imagemaker IT): Se modifica llamada a método getRangoPeriodo(int)
     *          por getRangoPeriodo(), esto para obtener todas las liquidaciones de sueldo del cliente,
     *          ya que anteriormente solo permitía la consulta de las liquidaciones de hasta 2 años de 
     *          antiguedad.</li>
     * <li>1.2  09/09/2008, Manuel Durán M. (Imagemaker IT): Se modifica interfaz, añadiendo el TO de cálculo 
     *          de renta para usarlo en las llamadas a  CalculaIngresosPorLiquidacion y CalculaPeriodosExigidos
     *          debido al cambio de stateful a stateless.</li>
     * </ul>
     *
     * @param evaluacion evaluación actual del cliente
     * @param historico indica si es una consulta de los históricos o un cálculo con más de un período
     * @param rentaTO TO de cálculo de renta
     * @throws wcorp.util.com.TuxedoException
     * @throws wcorp.serv.renta.CalculoRentaException
     * @throws java.rmi.RemoteException
     * @since ?
     */  
    private void cargaLiquidacionesDeSueldo(RentaCliente evaluacion, boolean historico, CalculoRentaTO rentaTO)
            throws CalculoRentaException, TuxedoException, RemoteException {

        log.debug("bprocess.CalculoRenta.cargaLiquidacionesDeSueldo");
        log.debug("************************************************");

        String rutCliente = evaluacion.getRutCliente();
        String dvRut = evaluacion.getDvRut();
        int numeroSecuencia = evaluacion.getNumeroSecuencia();

        /** Elias **/

        // toma valor 7 para el caso en que no ingreso la primera e ingreso 6
        String periodoConsulta = getRangoPeriodo(-7);
        /** Elias **/

        if (evaluacion.getOrigenCalculo() != null) {
            if (evaluacion.getOrigenCalculo().equals("LIQUIDACION")) {
                periodoConsulta = getRangoPeriodo();
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("El rango del periodo es : " + periodoConsulta);
        }
        if (log.isDebugEnabled()) {
            log.debug("El Rut y secuencia son: " + rutCliente + "-" + dvRut + ", secuencia=" + numeroSecuencia);
        }
        LiquidacionDeSueldo[] liquidaciones = trxBean.obtieneLiquidacionesDeSueldo(rutCliente, dvRut,
                numeroSecuencia, periodoConsulta);

        int len = liquidaciones == null ? 0 : liquidaciones.length;
        if (log.isDebugEnabled()) {
            log.debug("El numero de Liquidaciones registradas : " + len);
        }

        if (len > 0) {
            evaluacion.setRutEmpleador(liquidaciones[0].getRutEmpresa());
            evaluacion.setNombreEmpleador(liquidaciones[0].getRazonSocialEmpresa());
        }

        for (int i = 0; i < len; i++) {
            evaluacion.setLiquidacionSueldo(liquidaciones[i]);
            DetalleItem[] detalle = trxBean.obtieneDetalleLiquidacionesDeSueldo(rutCliente, dvRut,
                    liquidaciones[i].getNumeroSecuencia(), liquidaciones[i].getPeriodoLiquidacion()
                    + liquidaciones[i].getPeriodoLiquidacion());
            liquidaciones[i].setDetalleLiquidacion(detalle);
            liquidaciones[i].setGlosaLiquidacion(getGlosaPeriodo(liquidaciones[i].getPeriodoLiquidacion()));
            CalculaIngresosPorLiquidacion(liquidaciones[i], rentaTO);
            if (!historico) {
                rentaTO = CalculaPeriodosExigidos(rentaTO);
            }
            if (!historico) {
                if (liquidaciones[i].getPeriodoLiquidacion().compareToIgnoreCase(getPeriodoActualLiquidacion()) == 0) {
                    liquidaciones[i].setModificaPerfil(true);
                }
            } else {
                liquidaciones[i].setModificaPerfil(false);
            }
        }
        evaluacion.setPeriodosIngresados(len);
        rentaTO.setEvaluacionCliente(evaluacion);
    }

    /**
     * <p> Sobrecarga para el caso en que no es rescate de históricos  </p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>1.1  09/09/2008, Manuel Durán M. (Imagemaker IT): Se modifica interfaz, añadiendo el TO de cálculo 
     *          de renta, debido al cambio de stateful a stateless. Además se agrega javadoc.</li>
     * </ul>
     * 
     * @param rentaTO To de cálculo de renta
     * @throws wcorp.serv.renta.CalculoRentaException
     * @throws wcorp.util.com.TuxedoException
     * @throws java.rmi.RemoteException
     * @since ? 
     */ 
    private void cargaLiquidacionesDeSueldo(CalculoRentaTO rentaTO)
            throws CalculoRentaException, TuxedoException, RemoteException
            {
        cargaLiquidacionesDeSueldo(rentaTO.getEvaluacionCliente(), false, rentaTO);
            }

    /**
     * <p> Setea liquidacion actual en TO  </p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>1.1  09/09/2008, Manuel Durán M. (Imagemaker IT): Se modifica interfaz, añadiendo el TO de cálculo 
     *          de renta, debido al cambio de stateful a stateless. Para el caso en que la renta Total sea 
     *          igual a cero se setea en null la evaluacion del cliente del TO, el que sera recuperado y manejado 
     *          el error en servlet que lo invoca. Además se agrega javadoc.</li>
     * <li>1.2  01/06/2009, Eugenio Contreras (Imagemaker IT): Se añade mayor control sobre las excepciones que 
     *          puedan ocurrir al llamar al método {@link ServicioCalculoRentaBean#ingresaCalculoRenta(RentaCliente)}.
     *          Además en caso de que el cálculo de ingreso de una liquidación de sueldo sea 0, se arroja 
     *          una CalculoRentaException con un mensaje que descriva ésta situación.</li>
     * <li>1.3  30/09/2009, Eugenio Contreras (Imagemaker IT): Se reversa el cambio realizado en la versión 1.2 de éste
     *          método, debido a que éste estaba generando errores en el ingreso de las liquidaciones de los clientes.</li>
     * <li>1.4  20/04/2010, Miguel Sepúlveda G. (Imagemaker IT.): Se agrega manejo de excepcion a la llamada al metodo CalculaIngresosPorLiquidacion
     *          El cual enviara una excepcion en caso de que el calculo de ingresos obtenga valores negativos.</li>
     * </ul>
     * 
     * @param liquidacion Liquidación de sueldo a setear en TO
     * @param rentaTO To de cálculo de renta
     * @return el TO de CalculoRenta con datos calculados
     * @throws wcorp.serv.renta.CalculoRentaException
     * @since ? 
     */ 
    public CalculoRentaTO setLiquidacion(LiquidacionDeSueldo liquidacion, CalculoRentaTO rentaTO)
            throws CalculoRentaException {

        log.debug("*****CalculoRenta.setLiquidacion******");
        LiquidacionDeSueldo[] lSueldo = rentaTO.getEvaluacionCliente().getLiquidacionesDeSueldo();
        int respuestaServicio = 1;

        rentaTO.getEvaluacionCliente().setPeriodoDai(null);
        int len = lSueldo != null ? lSueldo.length : 0;

        // Se evalua que la liquidacion tenga alguna modificacion antes de hacer
        // el cambio en la Base de Datos
        if (len > 0) {
            LiquidacionDeSueldo lqSueldo = buscaLiquidacionPeriodo(liquidacion.getPeriodoLiquidacion(), rentaTO
                    .getEvaluacionCliente().getLiquidacionesDeSueldo());
            log.debug("Liquidacion de Sueldo Encontrada...");
            if (lqSueldo != null && !liquidacion.isModificado()) // Son iguales
                // por lo que
                // no realiza
                // la
                // Actualización
                return rentaTO;
        }

        // Realiza el Calculo de Ingreso para esta liquidacion
        try{
            CalculaIngresosPorLiquidacion(liquidacion, rentaTO);
        }catch(Exception ex)
        {
            if (log.isEnabledFor(Level.ERROR)) {
                log.error("[setLiquidacion] : Error al calcular ingresos por liquidación" + ex.getMessage());
            }
            throw new CalculoRentaException(ex.getMessage());
        }
        if (liquidacion.getTotalRLM() == 0) {
            log.debug("TotalRLM == 0, No se acepta la Liquidacion");
            rentaTO.setEvaluacionCliente(null);
            return rentaTO;
        }
        // Se verifica que la Evaluacion Exista, en caso que se nueva esta no
        // existe en el Sistema
        // Y lo primero que debe existir es la Evaluación con un numero de
        // secuencia.
        liquidacion.setModificado(false);
        try {
            // SOLO SI ES NUEVA
            if (rentaTO.getEvaluacionCliente().getNumeroSecuencia() == -1) {
                respuestaServicio = trxBean.ingresaCalculoRenta(rentaTO.getEvaluacionCliente());
                // ASIGNA NUMERO DE SECUENCIA LIQUIDACION CUANDO INGRESA LA
                // LIQUIDACION
                // EN CASO DE SER -1 (NUEVA)
                rentaTO.getEvaluacionCliente().setNumeroSecuencia(respuestaServicio);
            }

        } catch (Exception ex) {
            if (log.isEnabledFor(Level.ERROR)) {
                log.error("Problemas (bprocess.CalculoRenta.setLiquidacion) " + ex.getMessage());
            }
            throw new CalculoRentaException(ex.getMessage());
        }

        try {
            // NO ES UN NUEVO CALCULO DE RENTA.
            // INGRESAMOS ESTA NUEVA LIQUIDACION DE SUELDO ASIGNADA
            // A LA ACTUAL EVALUACION
            if (rentaTO.getEvaluacionCliente().getNumeroSecuencia() > -1) {
                liquidacion.setNumeroSecuencia(rentaTO.getEvaluacionCliente().getNumeroSecuencia());
                respuestaServicio = trxBean.ingresaLiquidacionSueldo(liquidacion);
                if (log.isDebugEnabled()) {
                    log.debug("ESTA ES LA RESPUESTA : " + respuestaServicio);
                }
                rentaTO.getEvaluacionCliente().setLiquidacionSueldo(liquidacion);
            } else {
                log
                .error("Problemas (bprocess.CalculoRenta.setLiquidacion) No hay Secuencia Asignada, la insercion RTA fallo");
                throw new CalculoRentaException("INSERCION_ULTIMO_CALCULO_FALLO");
            }
        } catch (Exception ex) {
            if (log.isEnabledFor(Level.ERROR)) {
                log.error("Problemas (bprocess.CalculoRenta.setLiquidacion) " + ex.getMessage());
            }
            throw new CalculoRentaException(ex.getMessage());
        }
        rentaTO = CalculaPeriodosExigidos(rentaTO);

        return rentaTO;
    }

    private LiquidacionDeSueldo buscaLiquidacionPeriodo(String periodo, LiquidacionDeSueldo[] liquidaciones) {
        int len = liquidaciones != null ? liquidaciones.length : 0;
        for (int i = 0; i < len; i++) {
            if (liquidaciones[i].getPeriodoLiquidacion().trim().compareToIgnoreCase(periodo) == 0) {
                if (log.isDebugEnabled()) {
                    log.debug("Existe...Devuelvo liquidacion : " + periodo + " corr:"
                            + liquidaciones[i].getNumeroSecuencia());
                }
                return liquidaciones[i];
            }
        }
        return null;
    }

    /**
     * <p> Obtiene la primera liquidacion de sueldo o setea una nueva en caso de no existir </p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>1.1  09/09/2008, Manuel Durán M. (Imagemaker IT): Se modifica interfaz, añadiendo el TO de cálculo 
     *          de renta, debido al cambio de stateful a stateless. Se elimina código comentado, además se 
     *          agrega javadoc.</li>
     * </ul>
     * 
     * @param ejecutivo Ejecutivo que ingresa datos del cliente
     * @param rentaTO To de cálculo de renta
     * @return Liquidacion de sueldo con seteo inicial
     * @throws wcorp.serv.renta.CalculoRentaException
     * @since ? 
     */ 
    public LiquidacionDeSueldo obtienePrimeraLiquidacionDeSueldo(String ejecutivo, CalculoRentaTO rentaTO)
            throws CalculoRentaException {

        log.debug("CalculoRenta.obtienePrimeraLiquidacionDeSueldo");

        LiquidacionDeSueldo lSueldo = buscaLiquidacionPeriodo(getPeriodoActualLiquidacion(), rentaTO
                .getEvaluacionCliente().getLiquidacionesDeSueldo());
        if (lSueldo != null) { // Existe la Liquidacion En la Evaluacion Actual
            if (lSueldo.getPeriodoLiquidacion().compareToIgnoreCase(getPeriodoActualLiquidacion()) == 0)
                lSueldo.setModificaPerfil(true);
            return lSueldo;
        }
        // Busca si existe una liquidacion para este periodo
        lSueldo = buscaLiquidacionPeriodo(getPeriodoActualLiquidacion(), rentaTO.getLiquidaciones());
        if (lSueldo != null) { // Existe la Liquidacion En la Evaluacion Actual
            if (lSueldo.getPeriodoLiquidacion().compareToIgnoreCase(getPeriodoActualLiquidacion()) == 0)
                lSueldo.setModificaPerfil(true);
            lSueldo.setNumeroSecuencia(rentaTO.getEvaluacionCliente().getNumeroSecuencia());
            return lSueldo;
        }

        // Si llega hasta aca, es porque la evaluacion no tiene asociada una
        // liquidacion y no hay una registrada

        lSueldo = new LiquidacionDeSueldo();
        lSueldo.setDvRut(rentaTO.getEvaluacionCliente().getDvRut());
        lSueldo.setNumeroSecuencia(rentaTO.getEvaluacionCliente().getNumeroSecuencia());

        /****/
        Date diaActual = new Date();
        Calendar rightNow2 = Calendar.getInstance();
        rightNow2.setTime(diaActual);
        if (log.isDebugEnabled()) {
            log.debug("rightNow2.get(Calendar.MONTH): " + rightNow2.get(Calendar.MONTH));
        }
        float diaLimite = 0;
        try {
            diaLimite = rentaTO.getParametrosCalculo().getDiaLimite();
        } catch (Exception ex) {
            if (log.isEnabledFor(Level.ERROR)) {
                log.error("Problemas (bprocess.CalculoRenta.obtieneParametrosDetalle) " + ex.getMessage());
            }
            throw new CalculoRentaException(ex.getMessage());
        }

        if (log.isDebugEnabled()) {
            log.debug("periodo actual: " + getPeriodoActualLiquidacion());
        }
        if (rightNow2.get(Calendar.DAY_OF_MONTH) > diaLimite) {
            log.debug("OJO Se debe optar por el mes anterior o el anterior a éste");
            lSueldo.setPeriodoLiquidacion(getPeriodoActualLiquidacion());
        } else {
            log.debug("OJO Se debe ingresar el mes anterior al anterior");
            lSueldo.setPeriodoLiquidacion(getPeriodoActualLiquidacion2());
        }
        if (log.isDebugEnabled()) {
            log.debug("OJO Periodo Liquidacion: " + lSueldo.getPeriodoLiquidacion());
        }
        /****/

        lSueldo.setGlosaLiquidacion(getGlosaPeriodo(lSueldo.getPeriodoLiquidacion()));
        lSueldo.setRutCliente(rentaTO.getEvaluacionCliente().getRutCliente());
        lSueldo.setRentaFija(0);
        lSueldo.setRentaVariable(0);
        lSueldo.setUsuarioIngreso(ejecutivo);
        lSueldo.setDetalleLiquidacion(obtieneParametrosDetalle("LIQUIDACION"));
        lSueldo.setModificaPerfil(true);

        return lSueldo;
    }

    /**
     * <p> Obtiene primera liquidacion para una nueva renta </p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>1.1  09/09/2008, Manuel Durán M. (Imagemaker IT): Se modifica interfaz, añadiendo el TO de cálculo 
     *          de renta, debido al cambio de stateful a stateless. Además se agrega javadoc.</li>
     * </ul>
     * 
     * @param ejecutivo Ejecutivo que ingresa datos del cliente
     * @param rentaTO To de cálculo de renta
     * @return Liquidacion de sueldo con seteo inicial
     * @throws wcorp.serv.renta.CalculoRentaException 
     * @since ? 
     */ 
    public LiquidacionDeSueldo obtienePrimeraLiquidacionLimite(String ejecutivo, CalculoRentaTO rentaTO)
            throws CalculoRentaException {

        log.debug("CalculoRenta.obtienePrimeraLiquidacionLimite");

        LiquidacionDeSueldo lSueldo = buscaLiquidacionPeriodo(getPeriodoActualLiquidacion(), rentaTO
                .getEvaluacionCliente().getLiquidacionesDeSueldo());
        if (lSueldo != null) { // Existe la Liquidacion En la Evaluacion Actual
            if (lSueldo.getPeriodoLiquidacion().compareToIgnoreCase(getPeriodoActualLiquidacion()) == 0)
                lSueldo.setModificaPerfil(true);
            return lSueldo;
        }
        // Busca si existe una liquidacion para este periodo
        lSueldo = buscaLiquidacionPeriodo(getPeriodoActualLiquidacion(), rentaTO.getLiquidaciones());
        if (lSueldo != null) { // Existe la Liquidacion En la Evaluacion Actual
            if (lSueldo.getPeriodoLiquidacion().compareToIgnoreCase(getPeriodoActualLiquidacion()) == 0)
                lSueldo.setModificaPerfil(true);
            lSueldo.setNumeroSecuencia(rentaTO.getEvaluacionCliente().getNumeroSecuencia());
            return lSueldo;
        }

        // Si llega hasta aca, es porque la evaluacion no tiene asociada una
        // liquidacion y no hay una registrada

        lSueldo = new LiquidacionDeSueldo();
        lSueldo.setDvRut(rentaTO.getEvaluacionCliente().getDvRut());
        lSueldo.setNumeroSecuencia(rentaTO.getEvaluacionCliente().getNumeroSecuencia());
        lSueldo.setPeriodoLiquidacion(getPeriodoActualLiquidacion2());
        lSueldo.setGlosaLiquidacion(getGlosaPeriodo(lSueldo.getPeriodoLiquidacion()));
        lSueldo.setRutCliente(rentaTO.getEvaluacionCliente().getRutCliente());
        lSueldo.setRentaFija(0);
        lSueldo.setRentaVariable(0);
        lSueldo.setUsuarioIngreso(ejecutivo);
        lSueldo.setDetalleLiquidacion(obtieneParametrosDetalle("LIQUIDACION"));
        lSueldo.setModificaPerfil(true);

        return lSueldo;
    }

    /**
     * <p> Obtiene siguiente liquidacion de sueldo para un perioso dado</p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>1.1  09/09/2008, Manuel Durán M. (Imagemaker IT): Se modifica interfaz, añadiendo el TO de cálculo 
     *          de renta, debido al cambio de stateful a stateless. Además se agrega javadoc.</li>
     * </ul>
     * 
     * @param periodo periodo que se desea buscar la siguiente liquidación de sueldo
     * @param ejecutivo Ejecutivo que ingresa datos del cliente
     * @param rentaTO To de cálculo de renta
     * @return Liquidacion de sueldo encontrada
     * @throws wcorp.serv.renta.CalculoRentaException 
     * @since ? 
     */ 
    public LiquidacionDeSueldo obtieneProximaLiquidacionDeSueldo(String periodo, String ejecutivo,
            CalculoRentaTO rentaTO) throws CalculoRentaException {

        log.debug("CalculoRenta.obtieneProximaLiquidacionDeSueldo");
        log.debug("**********************************************");

        if (log.isDebugEnabled()) {
            log.debug("BUSCO PROXIMO PERIODO DE : " + periodo);
        }
        String pxPeriodo = getProximoPeriodoLiquidacion(periodo);
        if (log.isDebugEnabled()) {
            log.debug("FECHA DEL PROXIMO PERIODO ES : " + pxPeriodo);
        }

        LiquidacionDeSueldo[] liquidaciones = rentaTO.getEvaluacionCliente().getLiquidacionesDeSueldo();
        int len = liquidaciones != null ? liquidaciones.length : 0;

        LiquidacionDeSueldo lSueldo = buscaLiquidacionPeriodo(pxPeriodo, rentaTO.getEvaluacionCliente()
                .getLiquidacionesDeSueldo());
        if (lSueldo != null) { // Existe la Liquidacion En la Evaluacion Actual
            if (lSueldo.getPeriodoLiquidacion().compareToIgnoreCase(getPeriodoActualLiquidacion()) == 0)
                lSueldo.setModificaPerfil(true);
            return lSueldo;
        }
        // Busca si existe una liquidacion para este periodo
        lSueldo = buscaLiquidacionPeriodo(pxPeriodo, rentaTO.getLiquidaciones());
        if (lSueldo != null) { // Existe la Liquidacion En la Evaluacion Actual
            lSueldo.setNumeroSecuencia(rentaTO.getEvaluacionCliente().getNumeroSecuencia());
            if (lSueldo.getPeriodoLiquidacion().compareToIgnoreCase(getPeriodoActualLiquidacion()) == 0)
                lSueldo.setModificaPerfil(true);
            return lSueldo;
        }

        // Si llega hasta aca, es porque la evaluacion no tiene asociada una
        // liquidacion y no hay una registrada

        lSueldo = new LiquidacionDeSueldo();
        lSueldo.setDvRut(rentaTO.getEvaluacionCliente().getDvRut());
        lSueldo.setNumeroSecuencia(rentaTO.getEvaluacionCliente().getNumeroSecuencia());
        lSueldo.setPeriodoLiquidacion(getProximoPeriodoLiquidacion(periodo));
        lSueldo.setGlosaLiquidacion(getGlosaPeriodo(lSueldo.getPeriodoLiquidacion()));
        lSueldo.setRutCliente(rentaTO.getEvaluacionCliente().getRutCliente());
        lSueldo.setRentaFija(0);
        lSueldo.setRentaVariable(0);
        lSueldo.setUsuarioIngreso(ejecutivo);
        lSueldo.setDetalleLiquidacion(obtieneParametrosDetalle("LIQUIDACION"));
        return lSueldo;
    }

    /**
     * <p> Obtiene la liquidación del periodo solicitada   </p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>1.1  09/09/2008, Manuel Durán M. (Imagemaker IT): Se modifica interfaz, añadiendo el listado 
     *          de liquidaciones, debido al cambio de stateful a stateless. Además se agrega javadoc.</li>
     * </ul>
     * 
     * @param periodo periodo de la liquidacion a buscar
     * @param liquidaciones liquidaciones asociadas al cálculo de renta
     * @return liquidacion encontrada del periodo indicado
     * @throws wcorp.serv.renta.CalculoRentaException
     * @since ? 
     */ 
    public LiquidacionDeSueldo obtieneLiquidacionDeSueldo(String periodo, LiquidacionDeSueldo[] liquidaciones)
            throws CalculoRentaException {

        int len = liquidaciones != null ? liquidaciones.length : 0;

        for (int i = 0; i < len; i++) {
            if (liquidaciones[i].getPeriodoLiquidacion().trim().compareTo(periodo) == 0) {
                if (liquidaciones[i].getPeriodoLiquidacion().compareToIgnoreCase(getPeriodoActualLiquidacion()) == 0)
                    liquidaciones[i].setModificaPerfil(true);
                return liquidaciones[i];
            }
        }
        return null;
    }

    /*
     * Obtiene Oficina del Colaborador
     * */

    public String obtieneGlosaOffColaborador(String codOff) throws CalculoRentaException, RemoteException {

        try {
            InitialContext ctx = JNDIConfig.getInitialContext();
            ServiciosMiscelaneosHome homeClt = (ServiciosMiscelaneosHome) ctx
                    .lookup("wcorp.serv.misc.ServiciosMiscelaneos");
            ServiciosMiscelaneos servMisc = homeClt.create();

            String glosaOficina = servMisc.pfiConsTabParam("OFI", codOff);
            if (glosaOficina == null)
                return "";
            else
                return glosaOficina;

        } catch (Exception e) {
            if( log.isEnabledFor(Level.ERROR) ){
                log.error("[obtieneGlosaOffColaborador] Ha ocurrido un error: " + e);
            }
            return "";
        }
    }



    /*
     * *********************
     * SERVICIOS PARA EL DAI
     * *********************
     */



    /**
     * <p> obtiene Dai del periodo actual</p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>1.1  09/09/2008, Manuel Durán M. (Imagemaker IT): Se elimina seteo de atributo dai de clase, el que
     *          deberá setearse directamente sobre el TO, en el metodo que lo llama, debido al cambio de stateful
     *          a stateless. Se elimina código comentado, además se agrega javadoc.</li>
     * </ul>
     * 
     * @param rutCliente rut del cliente
     * @param dvRut dígito verificador del cliente
     * @return DAI del período
     * @throws wcorp.serv.renta.CalculoRentaException
     * @throws wcorp.util.com.TuxedoException
     * @throws java.rmi.RemoteException
     * @since ? 
     */ 
    private Dai[] obtieneDaiDelPeriodo(String rutCliente, String dvRut) throws CalculoRentaException,
    TuxedoException, RemoteException {

        log.debug("bprocess.CalculoRenta.obtieneDaiDelPeriodo");
        log.debug("******************************************");

        int numeroSecuencia = -1;
        Dai[] dai = new Dai[3];

        // CARGO EL DAI DEL PERIODO ACTUAL
        String periodoDai = getPeriodoDai(getSaltoPeriodoDai());
        dai[0] = this.trxBean.obtieneDai(rutCliente, dvRut, numeroSecuencia, periodoDai);

        if (dai[0] != null) {
            if (log.isDebugEnabled()) {
                log.debug("TIENE HISTORIA DEL PERDIODO DAI 1: " + dai[0].getPeriodoDai() + "-"
                        + dai[0].getNumeroSecuencia());
            }
            DetalleItem[] detalle = this.trxBean.obtieneDetalleDai(rutCliente, dvRut, dai[0].getNumeroSecuencia(),
                    dai[0].getPeriodoDai());
            log.debug("SET DETALLE DAI 1***");
            if (log.isDebugEnabled()) {
                log.debug("getDetalleDai(): " + dai[0].getDetalleDai());
            }
            int largo = dai[0].getDetalleDai() != null ? dai[0].getDetalleDai().length : 0;
            if (largo == 0) {
                detalle = obtieneParametrosDetalle("DAI");
                int numDetalles = detalle != null ? detalle.length : 0;
                for (int d = 0; d < numDetalles; d++) {
                    detalle[d].setPorcentajeSobreRenta(0);
                }// for
            }// if
            dai[0].setDetalleDai(detalle);
        }// if

        // CARGO EL DAI DEL PERIODO ANTERIOR
        periodoDai = getPeriodoDai(getSaltoPeriodoDai() - 1);
        dai[1] = this.trxBean.obtieneDai(rutCliente, dvRut, numeroSecuencia, periodoDai);
        if (dai[1] != null) {
            if (log.isDebugEnabled()) {
                log.debug("TIENE HISTORIA DEL PERDIODO DAI 2: " + dai[1].getPeriodoDai() + "-"
                        + dai[1].getNumeroSecuencia());
            }
            DetalleItem[] detalle = this.trxBean.obtieneDetalleDai(rutCliente, dvRut, dai[1].getNumeroSecuencia(),
                    dai[1].getPeriodoDai());
            log.debug("SET DETALLE DAI 2***");
            dai[1].setDetalleDai(detalle);
            if (log.isDebugEnabled()) {
                log.debug("getDetalleDai(): " + dai[1].getDetalleDai());
            }
            int largo = dai[1].getDetalleDai() != null ? dai[1].getDetalleDai().length : 0;
            if (log.isDebugEnabled()) {
                log.debug("largo: " + largo);
            }
        }

        // CARGO EL DAI DEL PERIODO ANTERIOR ANTERIOR
        periodoDai = getPeriodoDai(getSaltoPeriodoDai() - 2);
        dai[2] = this.trxBean.obtieneDai(rutCliente, dvRut, numeroSecuencia, periodoDai);
        if (dai[2] != null) {
            if (log.isDebugEnabled()) {
                log.debug("TIENE HISTORIA DEL PERDIODO DAI 3: " + dai[2].getPeriodoDai() + "-"
                        + dai[2].getNumeroSecuencia());
            }
            DetalleItem[] detalle = this.trxBean.obtieneDetalleDai(rutCliente, dvRut, dai[2].getNumeroSecuencia(),
                    dai[2].getPeriodoDai());
            log.debug("SET DETALLE DAI 3***");
            dai[2].setDetalleDai(detalle);
            if (log.isDebugEnabled()) {
                log.debug("getDetalleDai(): " + dai[2].getDetalleDai());
            }
            int largo = dai[2].getDetalleDai() != null ? dai[2].getDetalleDai().length : 0;
            if (log.isDebugEnabled()) {
                log.debug("largo: " + largo);
            }
        }

        return dai;

    }

    /**
     * <p> Carga DAI en TO</p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>1.1  09/09/2008, Manuel Durán M. (Imagemaker IT): Se modifica interfaz, añadiendo el TO de cálculo 
     *          de renta, debido al cambio de stateful a stateless. Además se agrega javadoc.</li>
     * </ul>
     * 
     * @param evaluacion evaluación actual del cliente
     * @param historico indica si es una consulta de los históricos o un cálculo con más de un período
     * @param rentaTO TO de cálculo de renta
     * @throws wcorp.serv.renta.CalculoRentaException
     * @throws wcorp.util.com.TuxedoException
     * @throws java.rmi.RemoteException
     * @since ? 
     */ 
    private void cargaDai(RentaCliente evaluacion, boolean historico, CalculoRentaTO rentaTO)
            throws CalculoRentaException, TuxedoException, RemoteException {
        log.debug("bprocess.CalculoRenta.cargaDai");
        if (log.isDebugEnabled()) {
            log.debug("**************" + evaluacion.getPeriodoDai() + "****************");
        }

        String rutCliente = evaluacion.getRutCliente();
        String dvRut = evaluacion.getDvRut();
        int numeroSecuencia = evaluacion.getNumeroSecuencia();
        String periodoConsulta = evaluacion.getPeriodoDai();

        if (log.isDebugEnabled()) {
            log.debug("El rango del periodo es : " + periodoConsulta);
        }
        if (log.isDebugEnabled()) {
            log.debug("Rut Cliente: " + rutCliente + "-" + dvRut + ", numeroSecuencia=" + numeroSecuencia);
        }
        Dai dai = trxBean.obtieneDai(rutCliente, dvRut, numeroSecuencia, periodoConsulta);
        if (dai == null) {
            log.debug("EL DAI ES NULO");
        } else {
            log.debug("EL DAI NO ES NULO");
            if (log.isDebugEnabled()) {
                log.debug("El numero de secuencia segun el DAI es:[" + dai.getNumeroSecuencia() + "]");
            }
            if (log.isDebugEnabled()) {
                log.debug("El periodo segun el DAI es:[" + dai.getPeriodoDai() + "]");
            }
            DetalleItem[] detalle = trxBean.obtieneDetalleDai(rutCliente, dvRut, dai.getNumeroSecuencia(), dai
                    .getPeriodoDai());
            if (log.isDebugEnabled()) {
                log.debug("Detalle dai: " + detalle);
            }
            if (log.isDebugEnabled()) {
                log.debug("Detalle dai: " + detalle);
            }
            log.debug("SET DETALLE DAI 4***");
            if (log.isDebugEnabled()) {
                log.debug("** El numero de detalle es:[" + detalle.length + "] **");
            }
            for (int i = 0; i < detalle.length; i++) {
                log.debug("******* descripcion del detalle dai ************");
                if (log.isDebugEnabled()) {
                    log.debug("valor[" + detalle[i].getValorIngresado() + "]");
                }
                if (log.isDebugEnabled()) {
                    log.debug("Descripcion[" + detalle[i].getTms_ayd() + "]");
                }
                log.debug("******* Fin descripcion de detalle dai************");
            }
            dai.setDetalleDai(detalle);
            CalculaIngresosPorDai(dai, rentaTO);
            evaluacion.setDai(dai);
            rentaTO.setEvaluacionCliente(evaluacion);
        }
    }

    /**
     * <p> Sobrecarga para el caso en que no es rescate de históricos  </p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>1.1  09/09/2008, Manuel Durán M. (Imagemaker IT): SSe modifica interfaz, añadiendo el TO de cálculo 
     *          de renta, debido al cambio de stateful a stateless. Además se agrega javadoc.</li>
     * </ul>
     * 
     * @param rentaTO To de cálculo de renta
     * @throws wcorp.serv.renta.CalculoRentaException
     * @throws wcorp.util.com.TuxedoException
     * @throws java.rmi.RemoteException
     * @since ? 
     */ 
    private void cargaDai(CalculoRentaTO rentaTO)
            throws CalculoRentaException, TuxedoException, RemoteException
            {
        cargaDai(rentaTO.getEvaluacionCliente(), false, rentaTO);
            }

    private Dai buscaDaiPeriodo(String periodo, Dai[] dai) {

        int len = dai != null ? dai.length : 0;

        for (int i = 0; i < len; i++)
            if (dai[i] != null) {
                if (log.isDebugEnabled()) { log.debug("BUSCO DAI PERIODO : " + periodo); }
                if (log.isDebugEnabled()) { log.debug(dai.toString()); }
                if (dai[i].getPeriodoDai().trim().compareToIgnoreCase(periodo) == 0) {
                    if (log.isDebugEnabled()) { log.debug("Existe...Devuelvo liquidacion : " + periodo + " corr:" + dai[i].getNumeroSecuencia()); }
                    return dai[i];
                }
            }
        return null;
    }

    /**
     * <p> Obtiene primera DAI del periodo </p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>1.1  09/09/2008, Manuel Durán M. (Imagemaker IT): Se modifica interfaz, añadiendo el TO de cálculo 
     *          de renta y eliminando seteo en atributo de clase, debido al cambio de stateful a stateless. 
     *          Se elimina código comentado, además se agrega javadoc.</li>
     * </ul>
     * 
     * @param ejecutivo Ejecutivo que ingresa DAI
     * @param rentaTO To de cálculo de renta
     * @return Primera DAI para realizar cálculo
     * @throws wcorp.serv.renta.CalculoRentaException
     * @since ? 
     */ 
    public Dai obtienePrimeraDai(String ejecutivo, CalculoRentaTO rentaTO) throws CalculoRentaException {

        log.debug("CalculoRenta.obtienePrimeraDai");
        log.debug("******************************");

        if (log.isDebugEnabled()) {
            log.debug("OBTIENE PRIMERA DAI NUMERO SECUENCIA: 1.- "
                    + rentaTO.getEvaluacionCliente().getNumeroSecuencia() + "]");
        }
        Dai dai = rentaTO.getEvaluacionCliente().getDai();
        if (dai != null) { // Existe la Liquidacion En la Evaluacion Actual
            if (dai.getPeriodoDai().compareToIgnoreCase(getPeriodoDai(getSaltoPeriodoDai())) == 0) {
                log.debug("OBTIENE PRIMERA DAI 1.-");
                dai.setModificaPerfil(true);
                return dai;
            }
        }

        // Busca si existe un DAI para este periodo

        if (log.isDebugEnabled()) {
            log.debug("OBTIENE PRIMERA DAI NUMERO SECUENCIA: 2.- "
                    + rentaTO.getEvaluacionCliente().getNumeroSecuencia() + "]");
        }
        dai = buscaDaiPeriodo(getPeriodoDai(getSaltoPeriodoDai()), rentaTO.getDai());
        if (dai != null) { // Existe la Liquidacion En la Evaluacion Actual
            if (dai.getPeriodoDai().compareToIgnoreCase(getPeriodoDai(getSaltoPeriodoDai())) == 0) {
                dai.setModificaPerfil(true);
            }
            log.debug("OBTIENE PRIMERA DAI 2.-");
            dai.setNumeroSecuencia(rentaTO.getEvaluacionCliente().getNumeroSecuencia());
            return dai;
        }

        // Si llega hasta aca, es porque la evaluacion no tiene asociada una
        // liquidacion y no hay una registrada

        if (log.isDebugEnabled()) {
            log.debug("Se crea un DAI para el periodo : " + getPeriodoDai(getSaltoPeriodoDai()));
        }
        dai = new Dai();
        dai.setRutCliente(rentaTO.getEvaluacionCliente().getRutCliente());
        dai.setDvRut(rentaTO.getEvaluacionCliente().getDvRut());
        if (log.isDebugEnabled()) {
            log.debug("OBTIENE PRIMERA DAI NUMERO SECUENCIA: 3.- "
                    + rentaTO.getEvaluacionCliente().getNumeroSecuencia() + "]");
        }
        dai.setNumeroSecuencia(-1);
        dai.setPeriodoDai(getPeriodoDai(getSaltoPeriodoDai()));
        dai.setGlosaLiquidacion(dai.getPeriodoDai());
        dai.setRentaFija(0);
        dai.setRentaVariable(0);
        dai.setUsuarioIngreso(ejecutivo);

        DetalleItem detItem[] = obtieneParametrosDetalle("DAI");
        float suma_total_ingresos = calculaPorcentajeSobreRenta(dai);

        if (suma_total_ingresos != 0.0) {
            int numDetalles = detItem != null ? detItem.length : 0;
            for (int d = 0; d < numDetalles; d++) {
                float porcentaje = (detItem[d].getValorIngresado() * detItem[d].getTms_fct()) * 100
                        / suma_total_ingresos;
                detItem[d].setPorcentajeSobreRenta(porcentaje);
            }
        } else {
            int numDetalles = detItem != null ? detItem.length : 0;
            for (int d = 0; d < numDetalles; d++) {
                detItem[d].setPorcentajeSobreRenta(0);
            }
        }
        log.debug("SET DETALLE DAI 5***");
        dai.setDetalleDai(detItem);
        if (log.isDebugEnabled()) {
            log.debug("OBTIENE PRIMERA DAI NUMERO SECUENCIA: 4.- "
                    + rentaTO.getEvaluacionCliente().getNumeroSecuencia() + "]");
        }

        return dai;

    }

    /**
     * <p> Obtiene DAI del periodo indicado  </p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>1.1  09/09/2008, Manuel Durán M. (Imagemaker IT): Se modifica interfaz, añadiendo el TO de cálculo 
     *          de renta, debido al cambio de stateful a stateless. Además se agrega javadoc.</li>
     * <li>1.2  15/04/2014, Oscar Nahuelpan Alarcón (SEnTRA): Se inicializa atributo folioDeclaracion.</li>
     * </ul>
     * 
     * @param ejecutivo Ejecutivo que realiza cálculo de renta 
     * @param periodo periodo que se desea buscar
     * @param rentaTO To de cálculo de renta
     * @return DAI obtenido
     * @throws wcorp.serv.renta.CalculoRentaException 
     * @since ? 
     */
    public Dai obtieneDai(String ejecutivo, String periodo, CalculoRentaTO rentaTO) throws CalculoRentaException {

        getLogger().debug("CalculoRenta.obtieneDaiDelPeridodo");
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("************" + periodo + "******************");
        }

        Dai dai = rentaTO.getEvaluacionCliente().getDai();
        if (dai != null) { // Existe la Liquidacion En la Evaluacion Actual
            if (dai.getPeriodoDai().compareToIgnoreCase(periodo) == 0) {
                return dai;
            }
        }

        // Busca si existe un DAI para este periodo

        dai = buscaDaiPeriodo(periodo, rentaTO.getDai());
        if (dai != null) { // Existe la Liquidacion En la Evaluacion Actual
            if (dai.getPeriodoDai().compareToIgnoreCase(periodo) == 0)
                dai.setModificaPerfil(true);
            dai.setNumeroSecuencia(rentaTO.getEvaluacionCliente().getNumeroSecuencia());
            return dai;
        }

        // Si llega hasta aca, es porque la evaluacion no tiene asociada una
        // liquidacion y no hay una registrada

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Se crea un DAI para el periodo : " + periodo);
        }
        dai = new Dai();
        dai.setRutCliente(rentaTO.getEvaluacionCliente().getRutCliente());
        dai.setDvRut(rentaTO.getEvaluacionCliente().getDvRut());
        dai.setNumeroSecuencia(rentaTO.getEvaluacionCliente().getNumeroSecuencia());
        dai.setPeriodoDai(periodo);
        dai.setGlosaLiquidacion(dai.getPeriodoDai());
        dai.setRentaFija(0);
        dai.setRentaVariable(0);
        dai.setUsuarioIngreso(ejecutivo);
        dai.setFolioDeclaracion("0");

        DetalleItem detItem[] = obtieneParametrosDetalle("DAI");
        float suma_total_ingresos = calculaPorcentajeSobreRenta(dai);

        if (suma_total_ingresos != 0.0) {
            int numDetalles = detItem != null ? detItem.length : 0;
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("1.- Numero de parametros del DAI: " + numDetalles);
            }
            for (int d = 0; d < numDetalles; d++) {
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("1.- ValorIngresado: " + detItem[d].getValorIngresado());
                }
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("1.- Factor: " + detItem[d].getTms_fct());
                }
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("1.- Suma total de ingresos: " + suma_total_ingresos);
                }
                float porcentaje = (detItem[d].getValorIngresado() * detItem[d].getTms_fct()) * 100
                        / suma_total_ingresos;
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("1.- " + detItem[d].getTms_gls() + ", porcentaje=[" + porcentaje + "]");
                }
                detItem[d].setPorcentajeSobreRenta(porcentaje);
            }
        } else {
            int numDetalles = detItem != null ? detItem.length : 0;
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("2.- Numero de parametros del DAI: " + numDetalles);
            }
            for (int d = 0; d < numDetalles; d++) {
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("2.- " + detItem[d].getTms_gls() + ", porcentaje=[0]");
                }
                detItem[d].setPorcentajeSobreRenta(0);
            }
        }
        getLogger().debug("SET DETALLE DAI 6***");
        dai.setDetalleDai(detItem);

        return dai;

    }

    /**
     * <p> Setea el DAI indicado en el TO  </p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>1.1  09/09/2008, Manuel Durán M. (Imagemaker IT): Se modifica interfaz, añadiendo el TO de cálculo 
     *          de renta, debido al cambio de stateful a stateless. Se elimina código comentado, además se 
     *          agrega javadoc.</li>
     * <li>1.2  20/04/2010, Miguel Sepúlveda G (Imagemaker IT): Se agrega manejo de excepcion para la llamada al metodo CalculaIngresosPorDai, 
     *          el cual enviará una excepcion en caso de que el calculo de los ingresos tengan valores negativos.
     * </ul>
     * 
     * @param dai DAI a setear
     * @param rentaTO To de cálculo de renta
     * @return Evaluacion seteada con DAI a ingresar
     * @throws wcorp.serv.renta.CalculoRentaException 
     * @since ? 
     */
    public RentaCliente setDai(Dai dai, CalculoRentaTO rentaTO) throws CalculoRentaException {

        log.debug("*****CalculoRenta.setDai******");
        int respuestaServicio;

        // Se evalua que EL DAI tenga alguna modificacion antes de hacer el
        // cambio en la Base de Datos

        Dai daiRegistrado = rentaTO.getEvaluacionCliente().getDai();
        if (daiRegistrado != null) // Existe la Liquidacion En la Evaluacion
            // Actual
            if (daiRegistrado.getPeriodoDai().trim().compareToIgnoreCase(dai.getPeriodoDai().trim()) != 0
            && !dai.isModificado()) {
                log.debug("DAI NO HA SIDO MODIFICADO");
                return rentaTO.getEvaluacionCliente();
            }
        dai.setModificado(false);
        try
        {
            CalculaIngresosPorDai(dai, rentaTO);
        }catch(CalculoRentaException ex)
        {
            if(log.isEnabledFor(Level.ERROR)){
                log.error("[setDai] : Error al calcular los ingresos por dai " + ex.getMessage());
            }
            throw new CalculoRentaException(ex.getMessage());
        }
        try {
            rentaTO.getEvaluacionCliente().setPeriodoDai(dai.getPeriodoDai());
            rentaTO.getEvaluacionCliente().setPeriodoLiq(null);
            rentaTO.getEvaluacionCliente().setUsuarioActualiza(dai.getUsuarioActualiza());
            rentaTO.getEvaluacionCliente().setUsuarioIngreso(dai.getUsuarioIngreso());
            respuestaServicio = trxBean.ingresaCalculoRenta(rentaTO.getEvaluacionCliente());

            if (rentaTO.getEvaluacionCliente().getNumeroSecuencia() == -1) {
                rentaTO.getEvaluacionCliente().setNumeroSecuencia(respuestaServicio);
            }

        } catch (Exception ex) {
            if (log.isEnabledFor(Level.ERROR)) {
                log.error("Problemas (bprocess.CalculoRenta.setDai) " + ex.getMessage());
            }
            throw new CalculoRentaException(ex.getMessage());
        }

        try {
            if (log.isDebugEnabled()) {
                log.debug("Numero secuencia : " + rentaTO.getEvaluacionCliente().getNumeroSecuencia());
            }
            if (rentaTO.getEvaluacionCliente().getNumeroSecuencia() > -1) {
                dai.setNumeroSecuencia(rentaTO.getEvaluacionCliente().getNumeroSecuencia());
                if (log.isDebugEnabled()) {
                    log.debug("GRABANDO DAI DE EVALUACION: " + rentaTO.getEvaluacionCliente().getNumeroSecuencia()
                            + ", DAI=" + dai.getNumeroSecuencia());
                }
                if (log.isDebugEnabled()) {
                    log.debug("UIsuario Act: " + dai.getUsuarioActualiza());
                }
                if (log.isDebugEnabled()) {
                    log.debug("UIsuario Act: " + dai.getUsuarioIngreso());
                }
                respuestaServicio = trxBean.ingresaDai(dai);
                if (log.isDebugEnabled()) {
                    log.debug("ESTA ES LA RESPUESTA INGRESO DAI: " + respuestaServicio);
                }

                if (log.isDebugEnabled()) {
                    log.debug("Usuario Actualiza: " + rentaTO.getEvaluacionCliente().getUsuarioActualiza());
                }
                if (log.isDebugEnabled()) {
                    log.debug("Usuario Ingresa: " + rentaTO.getEvaluacionCliente().getUsuarioIngreso());
                }
                rentaTO.getEvaluacionCliente().setDai(dai);
            } else {
                log
                .error("Problemas (bprocess.CalculoRenta.setDat) No hay Secuencia Asignada, la insercion RTA fallo");
                throw new CalculoRentaException("INSERCION_ULTIMO_CALCULO_FALLO");
            }
        } catch (Exception ex) {
            if (log.isEnabledFor(Level.ERROR)) {
                log.error("Problemas (bprocess.CalculoRenta.setLiquidacion) " + ex.getMessage());
            }
            throw new CalculoRentaException(ex.getMessage());
        }

        log.debug("CALCULO INGRESOS POR DAI");

        rentaTO.getEvaluacionCliente().setPeriodosExigidos(1);
        rentaTO.getEvaluacionCliente().setPeriodosIngresados(1);
        return rentaTO.getEvaluacionCliente();
    }

    /**
     * <p> Obtiene los cálculos historicos  </p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>1.1  09/09/2008, Manuel Durán M. (Imagemaker IT): Se modifica interfaz, añadiendo la Evaluacion
     *          de renta del cliente, debido al cambio de stateful a stateless. Además se agrega javadoc.</li>
     * <li> 1.2 14/07/2010, Roberto Rodríguez(Imagemaker IT): Se agrega más información en los logs </li>
     * <li> 1.3 19/03/2015 Alejandro Barra (SEnTRA) - Andrés Alvarado (Ing. Soft. BCI):  se elimina variable logCalculo 
     *                                           , se agrega llamado a método getLogger, se modifica javadoc
     *                                           y se norma encabezado del método.</li>
     * </ul>
     * 
     * @param evaluacionCliente To de cálculo de renta
     * @return Calculos historicos obtenidos
     * @throws CalculoRentaException  en caso de haber algun error en el cálculo de renta.
     * @since ? 
     */
    public CalculoHistorico[] obtieneCalculoHistorico(RentaCliente evaluacionCliente)
            throws CalculoRentaException {
        if(getLogger().isEnabledFor(Level.INFO)){
            getLogger().info("[obtieneCalculoHistorico][BCI_INI]");
        }
        try {
            
            CalculoHistorico[] calculoHistorico = trxBean.calculoHistorico(evaluacionCliente.getRutCliente(),
                                                  evaluacionCliente.getDvRut());
            
            if(getLogger().isEnabledFor(Level.INFO)){
                getLogger().info("[obtieneCalculoHistorico][BCI_FINOK]");
            }
            return calculoHistorico;
        }
        catch (TuxedoException e) {
            if(getLogger().isEnabledFor(Level.ERROR)){
                getLogger().error("[obtieneCalculoHistorico][BCI_FINEX][TuxedoException] error con mensaje: "
                                   + e.getMessage(), e);
            }
            return null;
        }
        catch (RemoteException e) {
            if(getLogger().isEnabledFor(Level.ERROR)){
                getLogger().error("[obtieneCalculoHistorico][BCI_FINEX][RemoteException] error con mensaje: "
                                   + e.getMessage(), e);
            }
            return null;
        }
    }

    /**
     * <p> Calcula ingresos por DAI.</p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>1.1  09/09/2008, Manuel Durán M. (Imagemaker IT): Se modifica interfaz,añadiendo el TO de cálculo 
     *          de renta, debido al cambio de stateful a stateless. Se eliminan variables
     *          no usadas. Además se agrega javadoc.</li>
     * <li>1.2  20/04/2010, Miguel Sepúlveda G. (Imagemaker IT.): Se agrega la excepcion CalculoRentaException utilizada en caso
     *          de que el calculo de ingresos por DAI obtenga un resultado negativo, devolviendo una excepción con un mensaje informativo.
     *          Esto se realiza para validar que las rentas a guardar sean valores positivos.
     * <li>2.0  15/04/2014 Pedro Rebolledo Lagno (SEnTRA): Se modifica el calculo del total RLM.
     *                                                Se ajustan las líneas de logueo según la Normativa Vigente.
     * <li>2.1 04/11/2015, José Luis Allende (ImageMaker IT)-Rodrigo Ortiz (ing. Soft. BCI): Se verifica que se modifique dai antes de 
                   mandar error por cálculo negativo.</li>
     * </ul>
     * 
     * @param dai DAI del cliente
     * @param rentaTO To de cálculo de renta  
     * @throws CalculoRentaException CalculoRentaException.
     * @since ? 
     */
    private void CalculaIngresosPorDai(Dai dai, CalculoRentaTO rentaTO) throws CalculoRentaException {
    	if(getLogger().isInfoEnabled()) {getLogger().info("[CalculaIngresosPorDai][BCI_INI] Datos entrada: dai[" + dai + "]rentaTo["+rentaTO+"]");}
        DetalleItem[] detItem = dai.getDetalleDai();
        int len = detItem != null ? detItem.length : 0;

        if (getLogger().isDebugEnabled()) { getLogger().debug("[CalculaIngresosPorDai][Largo: " + len+"]"); }

        // Calcula Subtotal
        float subtotalIngresosBrutos = calculaSubTotalIngresosBrutos(dai);
        float totalLiquidoAnual = calculaTotalIngresoLiquidoAnualPercibido(dai);
        float totalIngresoFijos = calculaTotalIngresoFijos(dai);
        float totalIngresosVariables = calculaTotalIngresosVariables(dai);

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("[CalculaIngresosPorDai][INGRESOS LIQUIDO ANUAL  PERCIBIDO : " + totalLiquidoAnual+"]");
            getLogger().debug("[CalculaIngresosPorDai][INGRESOS FIJOS : " + totalIngresoFijos / TOTAL_MESES+"]");
            getLogger().debug("[CalculaIngresosPorDai][INGRESOS   VARIABLES : " + totalIngresosVariables / TOTAL_MESES+"]");
        }

        if(dai.isModificado() && (totalIngresoFijos < 0 || totalIngresosVariables < 0 
                || subtotalIngresosBrutos < 0 || totalLiquidoAnual < 0)){
            if(getLogger().isInfoEnabled()){
                getLogger().debug("[CalculaIngresosPorDai][BCI_FINOK][ERROR] "
                                   +"[Calculo de rentas negativas los valores correspondientes son: ");
                getLogger().debug("[Ingreso liquido anual : " + totalLiquidoAnual+"]");
                getLogger().debug("[Ingresos fijos : " + totalIngresoFijos+"]");
                getLogger().debug("[Ingresos variables : " + totalIngresosVariables+"]]");
            }
            throw new CalculoRentaException("Resultado de calculo con rentas negativas.");
        }

        double cod87 = getValorDetalleItem(dai.getDetalleDai(), "(Cod  87) Devolución de Imptos");
        double cod91 = getValorDetalleItem(dai.getDetalleDai(), "(Cod  91) Imptos pagados");
        
        double totalRLM = ((totalIngresoFijos + totalIngresosVariables) - (cod91 - cod87)) / TOTAL_MESES;
        
        if(getLogger().isDebugEnabled()){
            getLogger().debug("[CalculaIngresosPorDai][Total RLM : " + totalRLM+"]");
        }
        dai.setSubTotalIngresoBruto(subtotalIngresosBrutos);
        dai.setTotalIngresoAnual(totalLiquidoAnual);
        dai.setTotalIngresoFijoMensual(totalIngresoFijos / TOTAL_MESES);
        dai.setTotalIngresoVariableMensual(totalIngresosVariables / TOTAL_MESES);
        dai.setTotalRLM(new Float(totalRLM).floatValue());
        dai.setTotalRLMFINAL(dai.getTotalRLM() * calculaRLMFINAL(rentaTO));

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("[CalculaIngresosPorDai][RLM  : " + dai.getTotalRLM()+"]");
            getLogger().debug("[FACTOR RLM : " + calculaRLMFINAL(rentaTO)+"]");
            getLogger().debug("[RLM FINAL : " + dai.getTotalRLMFINAL()+"]");
        }

        dai.setTotalIngresosFijosSGC((dai.getTotalRLMFINAL() / dai.getTotalRLM())
                * dai.getTotalIngresoFijoMensual());
        dai.setTotalIngresosVariableSGC((dai.getTotalRLMFINAL() / dai.getTotalRLM())
                * dai.getTotalIngresoVariableMensual());

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("[CalculaIngresosPorDai][TotalIngresosFijosSGC: " + dai.getTotalIngresosFijosSGC()+"]");
            getLogger().debug("[TotalIngresosVariablesSGC: " + dai.getTotalIngresosVariableSGC()+"]");
        }

        if(getLogger().isInfoEnabled()) {getLogger().info("[CalculaIngresosPorDai][BCI_FINOK]");}
    }

    private float calculaPorcentajeSobreRenta(Dai dai)
    {
        float suma_ingresos = 0;

        suma_ingresos += getValorDetalleItem(dai.getDetalleDai(), "(Cod 104) Retiros");
        suma_ingresos += getValorDetalleItem(dai.getDetalleDai(), "(Cod 105) Distribución Dividendos");
        suma_ingresos += getValorDetalleItem(dai.getDetalleDai(), "(Cod 106) Gastos Rechazados");
        suma_ingresos += getValorDetalleItem(dai.getDetalleDai(), "(Cod 110) Boletas Honorarios");
        suma_ingresos += getValorDetalleItem(dai.getDetalleDai(), "(Cod 155) Intereses Cap. Mobiliarios");
        suma_ingresos += getValorDetalleItem(dai.getDetalleDai(), "(Cod 109) Rta Cont. Simplif. Contratos y Otros");
        suma_ingresos += getValorDetalleItem(dai.getDetalleDai(), "(Cod 152) Rtas exentas del Global");
        suma_ingresos += getValorDetalleItem(dai.getDetalleDai(), "(Cod 161) Sueldos, jubilación, etc");

        return suma_ingresos;
    }

    private float calculaSubTotalIngresosBrutos(Dai dai) {
        float s = 0;

        s += getValorDetalleItem(dai.getDetalleDai(), "(Cod 104) Retiros");
        s += getValorDetalleItem(dai.getDetalleDai(), "(Cod 105) Distribución Dividendos");
        s += getValorDetalleItem(dai.getDetalleDai(), "(Cod 106) Gastos Rechazados");
        s += getValorDetalleItem(dai.getDetalleDai(), "(Cod 110) Boletas Honorarios");
        s += getValorDetalleItem(dai.getDetalleDai(), "(Cod 155) Intereses Cap. Mobiliarios");
        s += getValorDetalleItem(dai.getDetalleDai(), "(Cod 109) Rta Cont. Simplif. Contratos y Otros");
        s += getValorDetalleItem(dai.getDetalleDai(), "(Cod 152) Rtas exentas del Global");
        s += getValorDetalleItem(dai.getDetalleDai(), "(Cod 161) Sueldos, jubilación, etc");

        s += getValorDetalleItem(dai.getDetalleDai(), "(Cod 169) Pérdidas en Cap. Mobiliarios");
        s += getValorDetalleItem(dai.getDetalleDai(), "(Cod 165) Impto de prim. categoria pagado año anterior");
        s += getValorDetalleItem(dai.getDetalleDai(), "(Cod 166) Impto territorial pagado en año anterior");
        s += getValorDetalleItem(dai.getDetalleDai(), "(Cod 111) Cotizaciones previsionales empresario o socio");

        return s;
    }

    private float calculaTotalIngresoLiquidoAnualPercibido(Dai dai) {
        float s = calculaSubTotalIngresosBrutos(dai);

        s += getValorDetalleItem(dai.getDetalleDai(), "(Cod  87) Devolución de Imptos");
        s += getValorDetalleItem(dai.getDetalleDai(), "(Cod  91) Imptos pagados");
        return s;
    }
    /**
     * Método calcular ingreso fijos.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 xx/xx/xxxx ?????????? ???????????? (????): Versión inicial.</li>
     * <li>2.0 15/04/2014 Rodrigo Pino (SEnTRA): Se debe modificar la actual fórmula de 
     *             cálculo ingresos variables, la cual genera valores negativos cuando el cliente posee muy 
     *             pocos ingresos variables y altos fijos.</li>
     * </ul>
     * <p>
     * 
     * @param dai Dai.
     * @return float renta.
     * @since 1.0
     */
    private float calculaTotalIngresoFijos(Dai dai) {
        float s = 0;

        s += getValorDetalleItem(dai.getDetalleDai(), "(Cod 161) Sueldos, jubilación, etc");
        s -= getValorDetalleItem(dai.getDetalleDai(), "(Cod 162) Crédito a favor 2ª categoría");
        
        return s;
    }
    /**
     * Método calcular ingreso variables.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 xx/xx/xxxx ?????????? ???????????? (????): Versión inicial.</li>
     * <li>2.0 15/04/2014 Rodrigo Pino (SEnTRA): Se debe modificar la actual fórmula de 
     *             cálculo ingresos variables, la cual genera valores negativos cuando el cliente posee muy 
     *             pocos ingresos variables y altos fijos.</li>
     * </ul>
     * <p>
     * @param dai Dai.
     * @return float renta.
     * @since 1.0
     */
    private float calculaTotalIngresosVariables(Dai dai) {
        float s = 0;

        s += getValorDetalleItem(dai.getDetalleDai(), "(Cod 104) Retiros");
        s += getValorDetalleItem(dai.getDetalleDai(), "(Cod 105) Distribución Dividendos");
        s += getValorDetalleItem(dai.getDetalleDai(), "(Cod 106) Gastos Rechazados");
        s += getValorDetalleItem(dai.getDetalleDai(), "(Cod 155) Intereses Cap. Mobiliarios");
        s += getValorDetalleItem(dai.getDetalleDai(), "(Cod 109) Rta Cont. Simplif. Contratos y Otros");
        s += getValorDetalleItem(dai.getDetalleDai(), "(Cod 152) Rtas exentas del Global");
        s -= getValorDetalleItem(dai.getDetalleDai(), "(Cod 169) Pérdidas en Cap. Mobiliarios");
        s -= getValorDetalleItem(dai.getDetalleDai(), "(Cod 165) Impto de prim. categoria pagado año anterior");
        s -= getValorDetalleItem(dai.getDetalleDai(), "(Cod 166) Impto territorial pagado en año anterior");
        s -= getValorDetalleItem(dai.getDetalleDai(), "(Cod 111) Cotizaciones previsionales empresario o socio");

        s = (float) (s * 0.8);

        s += getValorDetalleItem(dai.getDetalleDai(), "(Cod 110) Boletas Honorarios");

        return s;
    }

    /**
     * <p> Obtiene el factor de ajuste para realizar el cálculo de renta Final  </p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>1.1  09/09/2008, Manuel Durán M. (Imagemaker IT): Se modifica interfaz, añadiendo el TO de cálculo 
     *          de renta, debido al cambio de stateful a stateless. Además se agrega javadoc.</li>
     * </ul>
     * 
     * @param rentaTO To de cálculo de renta
     * @return Factor de ajuste para cálculo de renta Final 
     * @since ? 
     */
    private float calculaRLMFINAL(CalculoRentaTO rentaTO) {
        float factorAjuste = 1;

        // Cliente no es separado
        if (rentaTO.getEvaluacionCliente().getConyugeTrabaja().compareToIgnoreCase("N") == 0) {
            log.debug("Cuadro 2 : Dai No es Separado = 1");
            factorAjuste = getFactorCnyNoTrabaja(rentaTO.getEvaluacionCliente().getNumHijos(), rentaTO
                    .getParametrosCalculo());
        } else {// Cliente es separado
            // Cliente no tiene hijos
            if (rentaTO.getEvaluacionCliente().getConyugeTrabaja().compareToIgnoreCase("S") == 0
                    && rentaTO.getEvaluacionCliente().getNumHijos() == 0) {
                if (log.isDebugEnabled()) {
                    log.debug("Cuadro 2 : Dai Es Separado con 0???" + rentaTO.getEvaluacionCliente().getNumHijos()
                            + " Hijo = 1");
                }
                factorAjuste = 1;
            } else {// Cliente tiene hijos
                // Conyuge Trabaja
                if (rentaTO.getEvaluacionCliente().getConyugeTrabaja().compareToIgnoreCase("S") == 0) {
                    if (log.isDebugEnabled()) {
                        log.debug("Cuadro 2 : Dai Es Separado con " + rentaTO.getEvaluacionCliente().getNumHijos()
                                + " Hijo Trabaja ");
                    }
                    factorAjuste = getFactorCnyTrabaja(rentaTO.getEvaluacionCliente().getNumHijos(), rentaTO
                            .getParametrosCalculo());
                } else {// Conyuge no trabaja
                    if (log.isDebugEnabled()) {
                        log.debug("Cuadro 2 : Dai Es Separado con " + rentaTO.getEvaluacionCliente().getNumHijos()
                                + " Hijo Conyuge No Trabaja");
                    }
                    factorAjuste = getFactorCnyNoTrabaja(rentaTO.getEvaluacionCliente().getNumHijos(), rentaTO
                            .getParametrosCalculo());
                }
            }
        }
        return factorAjuste;
    }

    /**
     * <p> Calculo del DAI con boletas de honorarios </p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>1.1  09/09/2008, Manuel Durán M. (Imagemaker IT): Se modifica interfaz, añadiendo el TO de cálculo 
     *          de renta, debido al cambio de stateful a stateless. Además se agrega javadoc.</li>
     * <li>2.0  15/04/2014 Pedro Rebolledo Lagno (SEnTRA): Se agrega logica para el calculo de valores de boletas.
     *                                               Se ajustan las líneas de logueo según la Normativa Vigente.
     * <li>3.0  29/09/2014 Pedro Rebolledo Lagno (SEnTRA): Se elimina la lógica para el calculo de valores de 
     *                                                     boletas, agregada en la versión anterior.
     * </li>
     * </ul>
     * 
     * @param dai DAI del cliente
     * @param rentaTO To de cálculo de renta     
     * @since ? 
     */
    private void calculoDaiConBoletas(Dai dai, CalculoRentaTO rentaTO) {

        BoletaHonorarios[] boletas = rentaTO.getEvaluacionCliente().getBoletasHonorarios();
        int len = boletas == null ? 0 : boletas.length;
        // Calculo G26
        if (len >= TOPE_VALIDACION_BOLETAS) {
            getLogger().debug("G26 : Utiliza Valor de Boletas");
            dai.setG26(rentaTO.getEvaluacionCliente().getBhSustituirEnDai());
        }
		else {
            getLogger().debug("G26 : obtengo D12 de planilla DAI");
            dai.setG26(getValorDetalleItem(dai.getDetalleDai(), "(Cod 110) Boletas Honorarios"));
        }
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("G26: " + dai.getG26());
        }

        // Calculo G27
        dai.setG27(calcularG27(dai));
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("G27: " + dai.getG27());
        }

        // Calculo G28
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("TotalIngresoFijoMensual: " + dai.getTotalIngresoFijoMensual());
        }
        dai.setG28(dai.getTotalIngresoFijoMensual() + dai.getG27());
        dai.setG28((dai.getG28()
                +(getValorDetalleItem(dai.getDetalleDai(), "(Cod  91) Imptos pagados") 
                    + getValorDetalleItem(dai.getDetalleDai(), "(Cod  87) Devolución de Imptos"))) 
                    / CANTIDAD_COTIZACIONES);
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("G28: TotalIngresoFijoMensual + G27=" + dai.getG28());
        }

        // Calculo E33
        dai.setE33(dai.getTotalIngresoFijoMensual() / dai.getG28());
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("E33: " + dai.getE33());
        }

        // Calculo E34
        dai.setE34(dai.getG27() / dai.getG28());
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("E34: G27/G28=" + dai.getE34());
        }

        // Calculo Total Ingresos Fijos Mensuales multiplicado E33
        dai.setTotalRLMFINAL(dai.getTotalRLM() * calculaRLMFINAL(rentaTO));
        dai.setTotalIngresosMensualesPorE33(dai.getG28() * calculaRLMFINAL(rentaTO) * dai.getE33());
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("IngresosMensualesPorE33: G28 * E33=" + dai.getTotalIngresosMensualesPorE33());
        }

        // Calculo Total Ingresos Fijos Mensuales multiplicado E34
        dai.setTotalIngresosVariablesPorE34(dai.getG28() * calculaRLMFINAL(rentaTO) * dai.getE34());
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("IngresosVariablesPorE34: G28 * E34=" + dai.getTotalIngresosVariablesPorE34());
        }

    }

    private float calcularG27(Dai dai) {
        float s = 0;

        s += getValorDetalleItem(dai.getDetalleDai(), "(Cod 104) Retiros");
        s += getValorDetalleItem(dai.getDetalleDai(), "(Cod 105) Distribución Dividendos");
        s += getValorDetalleItem(dai.getDetalleDai(), "(Cod 106) Gastos Rechazados");
        s += getValorDetalleItem(dai.getDetalleDai(), "(Cod 155) Intereses Cap. Mobiliarios");
        s += getValorDetalleItem(dai.getDetalleDai(), "(Cod 109) Rta Cont. Simplif. Contratos y Otros");
        s += getValorDetalleItem(dai.getDetalleDai(), "(Cod 152) Rtas exentas del Global");
        s += getValorDetalleItem(dai.getDetalleDai(), "(Cod 169) Pérdidas en Cap. Mobiliarios");
        s += getValorDetalleItem(dai.getDetalleDai(), "(Cod 165) Impto de prim. categoria pagado año anterior");
        s += getValorDetalleItem(dai.getDetalleDai(), "(Cod 166) Impto territorial pagado en año anterior");
        s += getValorDetalleItem(dai.getDetalleDai(), "(Cod 111) Cotizaciones previsionales empresario o socio");

        if (log.isDebugEnabled()) { log.debug("SUMA PRIMER GRUPO: " + s); }
        s = (float) (s * 0.8);
        if (log.isDebugEnabled()) { log.debug("(SUMA PRIMER GRUPO)*0.8: " + s); }

        float g26 = 0;

        for (int i=0;i<12;i++){
            if (log.isDebugEnabled()) { log.debug("g26: " + g26); }
            g26 = g26 + dai.getG26();
        }

        //s += dai.getG26() * 12;
        s = s + g26;

        if (log.isDebugEnabled()) { log.debug("SUMA PRIMER GRUPO + G26(" + dai.getG26() + ")*12: " + s); }

        s += getValorDetalleItem(dai.getDetalleDai(), "(Cod  91) Imptos pagados");
        s += getValorDetalleItem(dai.getDetalleDai(), "(Cod  87) Devolución de Imptos");
        if (log.isDebugEnabled()) { log.debug("SUMA SEGUNDO GRUPO: " + s); }

        s = s / 12;
        return s;

    }

    /*
     * ***********************************
     * SERVICIOS PARA BOLETA DE HONORARIOS
     * ***********************************
     */

    /**
     * <p> Obtiene las boletas del periodo  </p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>1.1  09/09/2008, Manuel Durán M. (Imagemaker IT): Se elimina seteo de boletas en atributo
     *          de clase, por lo que deberán setearse desde donde se llama al método, esto para no  
     *          modificar interfaz, debido al cambio de stateful a stateless. Además se agrega javadoc.</li>
     * </ul>
     * 
     * @param rutCliente rut del cliente
     * @param dvRut Dígito verificador del cliente
     * @return Boletas del periodo del cliente
     * @throws wcorp.serv.renta.CalculoRentaException
     * @throws wcorp.util.com.TuxedoException
     * @throws java.rmi.RemoteException
     * @since ? 
     */
    private BoletaHonorarios[] obtieneBoletasDelPeriodo(String rutCliente, String dvRut)
            throws CalculoRentaException, TuxedoException, RemoteException {

        log.debug("bprocess.CalculoRenta.obtieneBoletasDelPeriodo");
        log.debug("**********************************************");

        int numeroSecuencia = -1;
        // CARGO BOLETA DE HONORARIOS
        String periodoBoletas = getRangoPeriodo(-12);

        BoletaHonorarios[] boletas = this.trxBean.obtieneBoletas(rutCliente, dvRut, numeroSecuencia,
                periodoBoletas);
        int numeroBoletas = boletas != null ? boletas.length : 0;
        if (log.isDebugEnabled()) {
            log.debug("NUMERO DE BOLETAS: " + numeroBoletas);
        }
        return boletas;
    }



    /**
     * <p> Carga todas las boletas de honorarios asociada a una liquidación de sueldo o DAI. Este método 
     *     modifica los atributos del objeto de tipo RentaCliente</p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>1.1  26/06/2007, Manuel Durán M. (Imagemaker IT): Se modifica llamada a método getRangoPeriodo(int) por 
     *          getRangoPeriodo(), esto para obtener todas las boletas asociadas a una liquidación de sueldo o Dai,
     *          ya que anteriormente solo permitía la consulta de las boletas dentro de un rango de hasta 2 años de antiguedad.</li>
     * <li>1.2  09/09/2008, Manuel Durán M. (Imagemaker IT): Se modifica interfaz, añadiendo el TO de cálculo 
     *          de renta, debido al cambio de stateful a stateless.</li>
     * </ul>
     *
     * @param evaluacion evaluación actual del cliente
     * @param historico indica si es una consulta de los históricos o un cálculo con más de un período
     * @param rentaTO To de cálculo de renta  
     * @throws wcorp.util.com.TuxedoException
     * @throws wcorp.serv.renta.CalculoRentaException
     * @throws java.rmi.RemoteException
     * @since ?
     */
    private void cargaBoletaHonorarios(RentaCliente evaluacion, boolean historico, CalculoRentaTO rentaTO)
            throws CalculoRentaException, TuxedoException, RemoteException {

        log.debug("bprocess.CalculoRenta.cargaBoletaHonorarios");
        log.debug("*******************************************");

        String rutCliente = evaluacion.getRutCliente();
        String dvRut = evaluacion.getDvRut();
        int numeroSecuencia = evaluacion.getNumeroSecuencia();
        String periodoConsulta = getRangoPeriodo();

        if (log.isDebugEnabled()) {
            log.debug("El rango del periodo es : " + periodoConsulta);
        }
        BoletaHonorarios[] boletas = trxBean.obtieneBoletas(rutCliente, dvRut, numeroSecuencia, periodoConsulta);
        int len = boletas == null ? 0 : boletas.length;
        if (log.isDebugEnabled()) {
            log.debug("El numero de Boletas registradas : " + len);
        }
        for (int i = 0; i < len; i++) {
            evaluacion.setBoletaHonorarios(boletas[i]);
        }
        CalculaIngresosPorBoletas(rentaTO);
    }

    /**
     * <p> Sobrecarga para el caso en que no es rescate e históricos  </p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>1.1  09/09/2008, Manuel Durán M. (Imagemaker IT): Se modifica interfaz, añadiendo el TO de cálculo 
     *          de renta, debido al cambio de stateful a stateless. Además se agrega javadoc.</li>
     * </ul>
     * 
     * @param rentaTO To de cálculo de renta
     * @throws wcorp.util.com.TuxedoException
     * @throws wcorp.serv.renta.CalculoRentaException
     * @throws java.rmi.RemoteException
     * @since ? 
     */ 
    private void cargaBoletaHonorarios(CalculoRentaTO rentaTO)
            throws CalculoRentaException, TuxedoException, RemoteException
            {
        cargaBoletaHonorarios(rentaTO.getEvaluacionCliente(), false, rentaTO);
            }

    /**
     * <p> Ingresa y setea el resultado de las boletas en la liquidacion  </p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>1.1  09/09/2008, Manuel Durán M. (Imagemaker IT): Se modifica interfaz, añadiendo el TO de cálculo 
     *          de renta, debido al cambio de stateful a stateless. Se debe usar el TO debido a la 
     *          llamada a CalculaIngresosPorBoletas. Además se agrega javadoc.</li>
     * </ul>
     * 
     * @param periodo periodo de la boleta de honorarios
     * @param monto monto de las boletas de honorarios
     * @param rentaTO To de cálculo de renta
     * @return TO de cálculo de renta
     * @throws CalculoRentaException
     * @throws TuxedoException
     * @throws RemoteException
     * @since ? 
     */ 
    public CalculoRentaTO setBoletaHonorarios(String periodo, float monto, CalculoRentaTO rentaTO)
            throws CalculoRentaException {

        log.debug("*****CalculoRenta.setBoletaHonorarios******");
        int respuestaServicio;

        BoletaHonorarios boleta = new BoletaHonorarios();

        boleta.setRutCliente(rentaTO.getEvaluacionCliente().getRutCliente());
        boleta.setDvRut(rentaTO.getEvaluacionCliente().getDvRut());
        boleta.setUsuarioIngreso(rentaTO.getEvaluacionCliente().getUsuarioIngreso());
        boleta.setNumeroSecuencia(rentaTO.getEvaluacionCliente().getNumeroSecuencia());
        boleta.setMontoBoleta(monto);
        boleta.setPeriodoBoleta(periodo);

        try {
            if (rentaTO.getEvaluacionCliente().getNumeroSecuencia() > -1) {
                respuestaServicio = trxBean.ingresaBoleta(boleta);
                if (log.isDebugEnabled()) {
                    log.debug("ESTA ES LA RESPUESTA : " + respuestaServicio);
                }
                rentaTO.getEvaluacionCliente().setBoletaHonorarios(boleta);
            } else {
                log
                .debug("Problemas (bprocess.CalculoRenta.setLiquidacion) No hay Calculo Asignada, la insercion Boleta fallo");
                throw new CalculoRentaException("INSERCION_ULTIMO_CALCULO_FALLO");
            }
        } catch (Exception ex) {
            if (log.isEnabledFor(Level.ERROR)) {
                log.error("Problemas (bprocess.CalculoRenta.setBoletaHonorarios) " + ex.getMessage());
            }
            throw new CalculoRentaException(ex.getMessage());
        }

        rentaTO = CalculaIngresosPorBoletas(rentaTO);
        return rentaTO;
    }

    /**
     * <p> Realiza el cálculo de ingresos incluyendo las boletas </p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>1.1  09/09/2008, Manuel Durán M. (Imagemaker IT): Se modifica interfaz, añadiendo el TO de cálculo 
     *          de renta, debido al cambio de stateful a stateless. Se elimina código comentado. Además se 
     *          agrega javadoc.</li>
     * <li>2.0  15/04/2014 Pedro Rebolledo Lagno (SEnTRA): Se modifica logica de obtencion de valor Boleta DAI.
     *                                             Se ajustan las líneas de logueo según la Normativa Vigente.
     * </li>
     * </ul>
     * 
     * @param evaluacion evaluación de renta del cliente
     * @param historicos indica si se trata de un cálculo de historico
     * @param rentaTO To de cálculo de renta
     * @return TO de cálculo de renta
     * @since ? 
     */
    private CalculoRentaTO CalculaIngresosPorBoletas(RentaCliente evaluacion, boolean historico,
            CalculoRentaTO rentaTO) {
        float mayorMonto = 0;

        BoletaHonorarios[] boletas = evaluacion.getBoletasHonorarios();
        int len = boletas == null ? 0 : boletas.length;
        float suma = 0;
        for (int i = 0; i < len; i++) {
            suma = suma + boletas[i].getMontoBoleta();
            if (boletas[i].getMontoBoleta() > mayorMonto)
                mayorMonto = boletas[i].getMontoBoleta();
        }

        evaluacion.setMontoTotalBoletas(suma);

        if (len > 0) {
            evaluacion.setPromedioMensualBoletas(suma / len);
        }

        evaluacion.setPromedioPonderadoBoletas(rentaTO.getEvaluacionCliente().getPromedioMensualBoletas()
                * rentaTO.getParametrosCalculo().getRVariable());

        // Desviacion estandar
        suma = 0;
        for (int i = 0; i < len; i++) {
            if (evaluacion.getPromedioMensualBoletas() > boletas[i].getMontoBoleta())
                suma = suma + (evaluacion.getPromedioMensualBoletas() - boletas[i].getMontoBoleta());
            else
                suma = suma + (boletas[i].getMontoBoleta() - evaluacion.getPromedioMensualBoletas());
        }
        if (len > 0) {
            evaluacion.setDesviacionEstandarBoletas(suma / len);
        }

        // Coeficiente de Variacion
        if (evaluacion.getPromedioMensualBoletas() > 0) {
            evaluacion.setCoeficienteVariacionBoletas(evaluacion.getDesviacionEstandarBoletas()
                    / evaluacion.getPromedioMensualBoletas());
        }

        /** @nuevo requerimiento: 11/11/2004 ************/
        /** 11/11/2004 Alexander (Apolo) ****************/
        // Subtotal de Boletas
        getLogger().debug("[CalculaIngresosPorBoletas]Mayor y Multiplique por Factor R. Variable : "
                + getFactorCV("Mayor y Multiplique por Factor R. Variable", rentaTO.getParametrosCalculo()));
        getLogger().debug("[CalculaIngresosPorBoletas]Promedio Ponderado : "
                + getFactorCV("Promedio Ponderado", rentaTO.getParametrosCalculo()));

        if (evaluacion.getCoeficienteVariacionBoletas() < getFactorCV(
                "Mayor y Multiplique por Factor R. Variable", rentaTO.getParametrosCalculo())) {
            evaluacion.setSubTotalBoletas(mayorMonto * rentaTO.getParametrosCalculo().getRVariable() / 1000);
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("[CalculaIngresosPorBoletas]SUBTOTAL BH : <11%" + mayorMonto + " * "
                        + rentaTO.getParametrosCalculo().getRVariable());
            }
        }

        if (evaluacion.getCoeficienteVariacionBoletas() >= getFactorCV(
                "Mayor y Multiplique por Factor R. Variable", rentaTO.getParametrosCalculo())
                && rentaTO.getEvaluacionCliente().getCoeficienteVariacionBoletas() <= getFactorCV(
                        "Promedio Ponderado", rentaTO.getParametrosCalculo())) {
            evaluacion.setSubTotalBoletas(evaluacion.getPromedioPonderadoBoletas() / 1000);
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("[CalculaIngresosPorBoletas]SUBTOTAL BH : >11% y < 30%"
                        + evaluacion.getPromedioPonderadoBoletas());
            }
        }
        if ((evaluacion.getCoeficienteVariacionBoletas() > getFactorCV("Promedio Ponderado", rentaTO
                .getParametrosCalculo()))
                && (rentaTO.getEvaluacionCliente().getCoeficienteVariacionBoletas() < 1.0)) {
            evaluacion.setSubTotalBoletas((evaluacion.getPromedioMensualBoletas() - evaluacion
                    .getDesviacionEstandarBoletas()) / 1000);
            if (getLogger().isDebugEnabled()) {
                getLogger()
                .debug("[CalculaIngresosPorBoletas]SUBTOTAL BH : > 30%"
                        + evaluacion.getPromedioMensualBoletas() + "-"
                        + evaluacion.getDesviacionEstandarBoletas());
            }
        }
        if (rentaTO.getEvaluacionCliente().getCoeficienteVariacionBoletas() >= 1.0) {
            evaluacion
            .setSubTotalBoletas(Float.parseFloat(getMontoBoletaMenor(rentaTO.getEvaluacionCliente())) / 1000);
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("[CalculaIngresosPorBoletas]SUBTOTAL BH : > 100%: "
                        + getMontoBoletaMenor(rentaTO.getEvaluacionCliente()));
            }

        }
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("[CalculaIngresosPorBoletas]---->TIPO DE CALCULO: " + evaluacion.getCodOrigen());
        }

        if (evaluacion.getDai() != null) {
            // BH a sustituir en DAI
            if (evaluacion.getMontoTotalBoletas() > 0) {
                float factor = getFactorMesesBH(len, rentaTO.getParametrosCalculo());
                Dai dai = evaluacion.getDai();
                float boletaHonorariosDai = getValorDetalleItem(dai.getDetalleDai(),
                        "(Cod 110) Boletas Honorarios");
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("[CalculaIngresosPorBoletas]BoletaHonorariosDai: " + boletaHonorariosDai);
                }
                float subTotalBH = evaluacion.getSubTotalBoletas() * 1000;
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("[CalculaIngresosPorBoletas]subTotalBH: " + subTotalBH);
                }
                float bhSustituirEnDai = (boletaHonorariosDai / 12) * (1 - factor) + (subTotalBH * factor);
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("[CalculaIngresosPorBoletas]1-factor: " + (1 - factor));
                }
                
                double valorBoletaDAI = getValorDetalleItem(dai.getDetalleDai(), "(Cod 110) Boletas Honorarios");
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("[CalculaIngresosPorBoletas] bhSustituirEnDai: ["+bhSustituirEnDai
                            +"], valorBoletaDAI:["+valorBoletaDAI+"]");
                }
                if(bhSustituirEnDai>valorBoletaDAI){
                    bhSustituirEnDai= new Float(valorBoletaDAI).floatValue();
                }
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("[CalculaIngresosPorBoletas] bhSustituirEnDai: ["+bhSustituirEnDai
                            +"] subTotalBH * factor: " + (subTotalBH * factor));
                }
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("[CalculaIngresosPorBoletas]bhSustituirEnDai: " + bhSustituirEnDai);
                }

                if (rentaTO.getEvaluacionCliente().getCoeficienteVariacionBoletas() > 1.0 && len >= 3) {
                    evaluacion.setBhSustituirEnDai(bhSustituirEnDai);
                } else if (len >= 3) {
                    evaluacion.setBhSustituirEnDai(bhSustituirEnDai);
                }
            }
        }

        if (evaluacion.getLiquidacionesDeSueldo() != null || evaluacion.getLiquidacionSueldo() != null) {
            // BH a sustituir en LIQUIDACION
            // 15-11-2004: se cambian tramos segun num de boletas(Está en el
            // capitulo 3.2.3 Actualización de BH del D01)
            if (len == 3)
                evaluacion.setBhActualizada(evaluacion.getSubTotalBoletas() * Float.parseFloat("0.5"));
            else if (len == 4)
                evaluacion.setBhActualizada(evaluacion.getSubTotalBoletas() * Float.parseFloat("0.7"));
            else if (len == 5)
                evaluacion.setBhActualizada(evaluacion.getSubTotalBoletas() * Float.parseFloat("0.9"));
            else
                evaluacion.setBhActualizada(evaluacion.getSubTotalBoletas() * Float.parseFloat("1"));
        }

        getLogger().debug("[CalculaIngresosPorBoletas]Calculo de Renta por Boletas");
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("[CalculaIngresosPorBoletas]BOLETA MONTO["+evaluacion.getMontoTotalBoletas()+"]");
        }
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("[CalculaIngresosPorBoletas]BOLETA PROMEDIO MENSUAL ["
                    + evaluacion.getPromedioMensualBoletas() + "]");
        }
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("[CalculaIngresosPorBoletas]BOLETA PROMEDIO PONDERADO ["
                    + evaluacion.getPromedioPonderadoBoletas() + "]");
        }
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("[CalculaIngresosPorBoletas]BOLETA DESVIACION ESTANDAR ["
                    + evaluacion.getDesviacionEstandarBoletas() + "]");
        }
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("[CalculaIngresosPorBoletas]BOLETA CV["+evaluacion.getCoeficienteVariacionBoletas()
                    + "]");
        }
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("[CalculaIngresosPorBoletas]BOLETA  SUBTOTAL["+evaluacion.getSubTotalBoletas()+"]");
        }
        if (getLogger().isDebugEnabled()) {
           getLogger().debug("[CalculaIngresosPorBoletas]BOLETA  BH EN DAI["+evaluacion.getBhSustituirEnDai()+"]");
        }
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("[CalculaIngresosPorBoletas]BOLETA  BH ACTUALIZADA ["+evaluacion.getBhActualizada() 
                    + "]");
        }
        rentaTO.setEvaluacionCliente(evaluacion);
        return (rentaTO);
    }

    /**
     * <p> Sobrecarga para el caso en que no es rescate e históricos  </p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>1.1  09/09/2008, Manuel Durán M. (Imagemaker IT): Se modifica interfaz, añadiendo el TO de cálculo 
     *          de renta, debido al cambio de stateful a stateless. Además se agrega javadoc.</li>
     * </ul>
     * 
     * @param rentaTO To de cálculo de renta
     * @return TO de cálculo de renta
     * @since ? 
     */ 
    private CalculoRentaTO CalculaIngresosPorBoletas(CalculoRentaTO rentaTO)
    {
        return CalculaIngresosPorBoletas(rentaTO.getEvaluacionCliente(), false, rentaTO);
    }

    /**
     * Método que setea los datos del cliente en el VO RentaCliente
     * <p>
     * @param rCliente renta del cliente sobre la cual se desea realizar los calculos
     * <p>
     * Registro de versiones:<ul>
     * <li>1.0 sin fecha Incorporacion - sin autor - versión inicial
     * <li>1.1 10-11-2005 - Emilio Fuentealba Silva (SEnTRA)- se agrega el
     * dato del canal por donde se realizan las modificaciones
     * <li>1.2 09/09/2008, Manuel Durán M. (Imagemaker IT): Se depreca debido a cambios de stateful a stateless, ya que 
     *         se deberan setear sobre el TO desde servlet de donde es llamado .</li>
     * </ul>
     * <p>
     * @since 1.0
     * @deprecated Datos se deberan setear desde servlets, ya que se elimina VO del bean debido a cambios de Stateful a Stateless
     */
    private void sumaBoletaHonorarios() {

    }

    /*
     * SERVICIOS PRIVADOS
     */

    private DetalleItem[] obtieneParametrosDetalle(String tipo) throws CalculoRentaException {

        log.debug("Ejecuto Servicio (bprocess.CalculoRenta.obtieneParametrosDetalle)  ");

        try {
            return trxBean.obtieneDetallePlantilla(tipo);
        } catch (Exception ex) {
            if( log.isEnabledFor(Level.ERROR) ){ 
                log.error("Problemas (bprocess.CalculoRenta.obtieneParametrosDetalle) " + ex.getMessage()); 
            }
            throw new CalculoRentaException(ex.getMessage());
        }

    }

    /**
     * <p> Calcula ingresos con las liquidaciones de sueldo.  </p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>1.1  09/09/2008, Manuel Durán M. (Imagemaker IT): Se modifica interfaz, añadiendo el TO de cálculo 
     *          de renta, debido al cambio de stateful a stateless. Además se agrega javadoc.</li>
     * <li>1.2  20/04/2010, Miguel Sepúlveda G. (Imagemaker IT): Se agrega la exception CalculoRentaException.
     *          Utilizada para validar que el calculo de ingresos obtenga valores positivos en el calculo, en caso de que
     *          den valores negativos, este devolvera una exception con  un mensaje informativo.
     * <li>2.0  15/04/2014 Rodrigo Pino (SEnTRA): Se agrega validacion para evaluar el menor valor entre los 
     *             valores de asignacion dados por el ejecutivo y el tope maximo que deben tener dichos valores.
     *                                            Se ajustan las líneas de logueo según la Normativa Vigente.
     * <li>2.1  29/09/2014 Pedro Rebolledo (SEnTRA): Se corrige la lógica que valida si se exede el tope maximo
     *                                               ingresado.
     *  <li>2.2 04/11/2015, José Luis Allende (ImageMaker IT)-Rodrigo Ortiz (ing. Soft. BCI): Se verifica que liquidación 
     *                      haya sido modificada antes de mandar error por cálculo negativo.</li>
     * </ul>
     * 
     * @param liquidacion Liquidacion de sueldo del cliente
     * @param rentaTO To de cálculo de renta
     * @throws CalculoRentaException CalculoRentaException.
     * @since ? 
     */
    private void CalculaIngresosPorLiquidacion(LiquidacionDeSueldo liquidacion, CalculoRentaTO rentaTO)
            throws CalculoRentaException{
    	if(getLogger().isInfoEnabled()) {getLogger().info("[CalculaIngresosPorLiquidacion][BCI_INI] Datos entrada: liquidacion[" + liquidacion + "]rentaTO["+rentaTO+"]");}
        float ingresosFijos = 0;
        float descuentosFijos = 0;
        float ingresosVariables = 0;

        DetalleItem[] detItem = liquidacion.getDetalleLiquidacion();
        int len = detItem != null ? detItem.length : 0;

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("[CalculaIngresosPorLiquidacion][Largo: " + len+"]");
        }
        
        String topeMaximoAsignacion = 
            TablaValores.getValor(TABLA_PARAMETROS_CALCULORENTA,"TopeMaximoAsignacion","valor");
        String camposAsignaciones = 
                TablaValores.getValor(TABLA_PARAMETROS_CALCULORENTA,"CamposAsignaciones","codigos");

        
        if (log.isDebugEnabled()) {
            log.debug("[CalculaIngresosPorLiquidacion][Largo: " + len+"]");
        }
        
        boolean valorIngTope = Boolean.FALSE.booleanValue();

        for (int i = 0; i < len; i++) {
            DetalleItem detalle = detItem[i];
            
            if (StringUtil.cuentaOcurrencias(String.valueOf(detalle.getTms_clv()).trim()
                    , camposAsignaciones)>0) {
            if (detalle.getValorIngresado() > Float.parseFloat(topeMaximoAsignacion)){
                    valorIngTope = Boolean.TRUE.booleanValue();
            }
            }

            if(valorIngTope){
                if (detalle.getTms_tip().trim().compareToIgnoreCase("F") == 0) {
                    if (detalle.getTms_fct() >= 0) {
                        ingresosFijos = ingresosFijos
                                        + Float.parseFloat(topeMaximoAsignacion) * detalle.getTms_fct();
                    } 
                    else {
                        descuentosFijos = descuentosFijos
                                          + Float.parseFloat(topeMaximoAsignacion) * detalle.getTms_fct();
                    }
                }

                if (detalle.getTms_tip().trim().compareToIgnoreCase("V") == 0) {
                    if (detalle.getTms_fct() >= 0) {
                        ingresosVariables = ingresosVariables
                                            + Float.parseFloat(topeMaximoAsignacion) * detalle.getTms_fct();
                    }
                }
            }
            else{
            if (detalle.getTms_tip().trim().compareToIgnoreCase("F") == 0) {
                if (detalle.getTms_fct() >= 0) {
                    ingresosFijos = ingresosFijos + detalle.getValorIngresado() * detalle.getTms_fct();
                } 
                else {
                    descuentosFijos = descuentosFijos + detalle.getValorIngresado() * detalle.getTms_fct();
                }
            }
            if (detalle.getTms_tip().trim().compareToIgnoreCase("V") == 0) {
                if (detalle.getTms_fct() >= 0) {
                    ingresosVariables = ingresosVariables + detalle.getValorIngresado() * detalle.getTms_fct();
                }
            }
        }

            valorIngTope = Boolean.FALSE.booleanValue();
        }

        liquidacion.setIngresosFijos(ingresosFijos);
        liquidacion.setIngresosVariables(ingresosVariables);
        liquidacion.setTotalDescuentos(descuentosFijos);

        liquidacion = calculaSubtotalesLiquidacion(liquidacion);
        liquidacion = calculaTotalesLiquidacion(liquidacion);

        ajustarRLMLiquidacion(liquidacion, rentaTO);

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("[CalculaIngresosPorLiquidacion][INGRESOS FIJOS[" + liquidacion.getIngresosFijos() 
                    + "] INGRESOS VARIABLES [" + liquidacion.getIngresosVariables() 
                    + "] DESCUENTOS [" + liquidacion.getTotalDescuentos() 
                    + "] SUBTOTAL INGRESO BRUTO [" + liquidacion.getSubTotalIngresoBruto() 
                    + "] SUBTOTAL INGRESO FIJO [" + liquidacion.getSubTotalIngresoFijo() 
                    + "] SUBTOTAL INGRESO VARIABLE [" + liquidacion.getSubTotalIngresoVariable() 
                    + "] TOTAL INGRESO FIJO [" + liquidacion.getTotalIngresosFijos() 
                    + "] TOTAL INGRESO VARIABLE [" + liquidacion.getTotalIngresosVariables() 
                    + "] % INGRESO FIJO [" + liquidacion.getPorcentajeIngresosFijos() 
                    + "] % INGRESO VARIABLE [" + liquidacion.getPorcentajeIngresosVariables() 
                    + "] TOTAL RLM [" + liquidacion.getTotalRLM() 
                    + "] TOTAL RLM INICIAL [" + liquidacion.getTotalRLMajustadaInicial() 
                    + "] TOTAL RLM AJUSTADA [" + liquidacion.getTotalRLMajustada() + "]]");
        }
        if(liquidacion.isModificado() && (liquidacion.getTotalIngresosFijos() < 0 || liquidacion.getTotalIngresosVariables() < 0)){
            if(getLogger().isInfoEnabled()){
                getLogger().debug("[CalculaIngresosPorLiquidacion][BCI_FINOK][ERROR][Calculo ingresos liquidación valores negativos,"
                    +" datos :  Total ingresos fijos " + liquidacion.getTotalIngresosFijos()+"]");
                getLogger().debug("[CalculaIngresosPorLiquidacion][Total ingresos variables " 
                    + liquidacion.getTotalIngresosVariables()+"]");
            }
            throw new CalculoRentaException("Resultado de calculo de ingresos por liquidación con valores negativos");
        }
        if(getLogger().isInfoEnabled()) {getLogger().info("[CalculaIngresosPorLiquidacion][BCI_FINOK]");}
    }

    private LiquidacionDeSueldo calculaSubtotalesLiquidacion(LiquidacionDeSueldo lSueldo) {

        log.debug("calculaSubtotalesLiquidacion");

        lSueldo.setSubTotalIngresoBruto(lSueldo.getIngresosFijos() + lSueldo.getIngresosVariables());
        if (lSueldo.getSubTotalIngresoBruto() == 0) {
            lSueldo.setSubTotalIngresoFijo(0);
            lSueldo.setSubTotalIngresoVariable(0);
        } else {
            lSueldo.setSubTotalIngresoFijo((float) (lSueldo.getIngresosFijos() / lSueldo.getSubTotalIngresoBruto()));
            lSueldo.setSubTotalIngresoVariable((float) (lSueldo.getIngresosVariables() / lSueldo.getSubTotalIngresoBruto()));
        }

        return lSueldo;
    }

    private LiquidacionDeSueldo calculaTotalesLiquidacion(LiquidacionDeSueldo lSueldo) {

        log.debug("calculaTotalesLiquidacion");

        if (log.isDebugEnabled()) { log.debug("lSueldo.getTotalDescuentos()= " + lSueldo.getTotalDescuentos()); }
        lSueldo.setTotalIngresosFijos(lSueldo.getIngresosFijos() + (lSueldo.getTotalDescuentos() * lSueldo.getSubTotalIngresoFijo()));
        lSueldo.setTotalIngresosVariables((float) (lSueldo.getIngresosVariables() * 1 + (lSueldo.getTotalDescuentos() * lSueldo.getSubTotalIngresoVariable())));
        lSueldo.setTotalRLM(lSueldo.getTotalIngresosFijos() + lSueldo.getTotalIngresosVariables());
        if (lSueldo.getTotalRLM() == 0) {
            lSueldo.setPorcentajeIngresosFijos(0);
            lSueldo.setPorcentajeIngresosVariables(0);
        } else {
            lSueldo.setPorcentajeIngresosFijos(lSueldo.getTotalIngresosFijos() / lSueldo.getTotalRLM());
            lSueldo.setPorcentajeIngresosVariables(lSueldo.getTotalIngresosVariables() / lSueldo.getTotalRLM());
        }

        return lSueldo;
    }

    /**
     * <p> Ajusta la renta final de acuerdo a los parametros de ajuste</p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>1.1  09/09/2008, Manuel Durán M. (Imagemaker IT): Se modifica interfaz, añadiendo el TO de cálculo 
     *          de renta, debido al cambio de stateful a stateless. Además se agrega javadoc.</li>
     * </ul>
     * 
     * @param lSueldo liquidacion de sueldo del cliente
     * @param rentaTO To de cálculo de renta
     * @since ? 
     */
    private void ajustarRLMLiquidacion(LiquidacionDeSueldo lSueldo, CalculoRentaTO rentaTO) {
        RentaCliente cRenta = rentaTO.getEvaluacionCliente();
        float factorAjuste = 1;

        lSueldo.setTotalRLMajustada(lSueldo.getTotalRLM());

        if (log.isDebugEnabled()) {
            log.debug("Es personal de las FFAA: " + cRenta.getPerteneceFFAA());
        }

        if (cRenta.getPerteneceFFAA().compareToIgnoreCase("S") == 0) {
            String grado = cRenta.getGradoFFAA();
            factorAjuste = getFactorFFAA(grado, rentaTO.getParametrosCalculo());
            if (log.isDebugEnabled()) {
                log.debug("ajustarRLMLiquidacion : FACTOR AJUSTE GRADO FFAA : (" + grado + ")" + factorAjuste);
            }
            if (log.isDebugEnabled()) {
                log.debug("TotalRLMajustadaInicial = " + (lSueldo.getTotalRLM() * factorAjuste));
            }
            lSueldo.setTotalRLMajustadaInicial(lSueldo.getTotalRLM() * factorAjuste);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("TotalRLMajustadaInicial = " + lSueldo.getTotalRLM());
            }
            lSueldo.setTotalRLMajustadaInicial(lSueldo.getTotalRLM());
        }

        if (log.isDebugEnabled()) {
            log.debug("Es Separado : " + cRenta.getEstadoCivil());
        }
        if (log.isDebugEnabled()) {
            log.debug("Retención Judicial : "
                    + getValorDetalleItem(lSueldo.getDetalleLiquidacion(), "Retención Judicial"));
        }

        if (cRenta.getEstadoCivil().compareToIgnoreCase("N") == 0) {
            lSueldo.setTotalRLMajustada(lSueldo.getTotalRLMajustadaInicial());
        }

        if (cRenta.getNumHijos() == 0 && cRenta.getEstadoCivil().compareToIgnoreCase("S") == 0) {
            log.debug("Numero Hijos == 0 && EstadoCivil == S");
            if (log.isDebugEnabled()) {
                log.debug("TotalRLMajustada = " + lSueldo.getTotalRLMajustadaInicial());
            }
            lSueldo.setTotalRLMajustada(lSueldo.getTotalRLMajustadaInicial());
        }

        if (Math.abs(getValorDetalleItem(lSueldo.getDetalleLiquidacion(), "Retención Judicial")) > 0
                && cRenta.getEstadoCivil().compareToIgnoreCase("S") == 0) {
            log.debug("Retención Judicial > 0");
            if (log.isDebugEnabled()) {
                log.debug("Estado Civil: " + cRenta.getEstadoCivil().compareToIgnoreCase("S"));
            }
            if (log.isDebugEnabled()) {
                log.debug("TotalRLMajustada = " + lSueldo.getTotalRLMajustadaInicial());
            }
            lSueldo.setTotalRLMajustada(lSueldo.getTotalRLMajustadaInicial());
        }

        if (Math.abs(getValorDetalleItem(lSueldo.getDetalleLiquidacion(), "Retención Judicial")) == 0
                && cRenta.getEstadoCivil().compareToIgnoreCase("S") == 0) {
            log.debug("Retención Judicial == 0");
            if (log.isDebugEnabled()) {
                log.debug("Estado Civil: " + cRenta.getEstadoCivil().compareToIgnoreCase("S"));
            }
            if (cRenta.getConyugeTrabaja().compareToIgnoreCase("S") == 0) {
                factorAjuste = getFactorCnyTrabaja(cRenta.getNumHijos(), rentaTO.getParametrosCalculo());
                if (log.isDebugEnabled()) {
                    log.debug("ajustarRLMLiquidacion : FACTOR EX TRABAJA : " + factorAjuste);
                }
                if (log.isDebugEnabled()) {
                    log.debug("TotalRLMajustada = " + (lSueldo.getTotalRLMajustadaInicial() * factorAjuste));
                }
                lSueldo.setTotalRLMajustada(lSueldo.getTotalRLMajustadaInicial() * factorAjuste);
            } else {
                factorAjuste = getFactorCnyNoTrabaja(cRenta.getNumHijos(), rentaTO.getParametrosCalculo());
                if (log.isDebugEnabled()) {
                    log.debug("ajustarRLMLiquidacion : FACTOR EX NO TRABAJA : " + factorAjuste);
                }
                if (log.isDebugEnabled()) {
                    log.debug("TotalRLMajustada = " + (lSueldo.getTotalRLMajustadaInicial() * factorAjuste));
                }
                lSueldo.setTotalRLMajustada(lSueldo.getTotalRLMajustadaInicial() * factorAjuste);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("TotalRLMajustada FINAL = " + lSueldo.getTotalRLMajustada());
        }
    }

    private float getFactorDetalleItem(DetalleItem[] detalle, String glosa) {

        int len = detalle != null ? detalle.length : 0;
        for (int i = 0; i < len; i++) {
            if (detalle[i].getTms_gls().trim().compareToIgnoreCase(glosa) == 0)
                return detalle[i].getTms_fct();
        }
        return 0;
    }

    private float getValorDetalleItem(DetalleItem[] detalle, String glosa) {

        int len = detalle != null ? detalle.length : 0;
        for (int i = 0; i < len; i++) {
            if (detalle[i].getTms_gls().trim().compareToIgnoreCase(glosa) == 0) {
                if (log.isDebugEnabled()) { log.debug("getValorDetalleItem(" + detalle[i].getTms_gls() + ", val=" + detalle[i].getValorIngresado() + ", fct=" + detalle[i].getTms_fct() + ", ret=" + (detalle[i].getValorIngresado() * detalle[i].getTms_fct())); }
                return detalle[i].getValorIngresado() * detalle[i].getTms_fct();
            }
        }
        return 0;
    }

    /**
     * <p> Obtiene el factor de FFAA </p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>1.1  09/09/2008, Manuel Durán M. (Imagemaker IT): Se modifica interfaz, añadiendo los parametros de
     *          cálculo, debido al cambio de stateful a stateless. Además se agrega javadoc.</li>
     * </ul>
     * 
     * @param id el grado de fuerza armada
     * @param parametrosCalculo parametros de cálculo
     * @return el factor correspondiente del cargo en FFAA 
     * @since ? 
     */
    private float getFactorFFAA(String id, Constantes parametrosCalculo) {

        float factor = 1;
        Parametros[] listFFAA = parametrosCalculo.getGradoFFAA();
        int len = listFFAA != null ? listFFAA.length : 0;
        for (int i = 0; i < len; i++) {
            if (log.isDebugEnabled()) {
                log.debug("CLAVE FFAA [" + listFFAA[i].getTms_clv() + "][" + id + "]");
            }
            if (listFFAA[i].getTms_clv().trim().equals(id.trim())) {
                factor = listFFAA[i].getTms_fct();
            }
        }

        return factor;
    }

    /**
     * <p> Obtiene el factor para conyuge que trabaja </p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>1.1  09/09/2008, Manuel Durán M. (Imagemaker IT): Se modifica interfaz, añadiendo los parametros de
     *          cálculo, debido al cambio de stateful a stateless. Además se agrega javadoc.</li>
     * </ul>
     * 
     * @param id el grado de fuerza armada
     * @param parametrosCalculo parametros de cálculo
     * @return el factor correspondiente a conyuge que trabaja
     * @since ? 
     */
    private float getFactorCnyTrabaja(int id, Constantes parametrosCalculo) {

        float factor = 1;
        Parametros[] lista = parametrosCalculo.getCnyTrabaja();
        int len = lista != null ? lista.length : 0;
        for (int i = 0; i < len; i++) {
            if (Integer.parseInt(lista[i].getTms_clv().trim()) == id)
                factor = lista[i].getTms_fct();
        }
        return factor;
    }

    /**
     * <p> Obtiene el factor para conyuge que no trabaja  </p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>1.1  09/09/2008, Manuel Durán M. (Imagemaker IT): Se modifica interfaz, añadiendo los parametros de
     *          cálculo, debido al cambio de stateful a stateless. Además se agrega javadoc.</li>
     * </ul>
     * 
     * @param id el grado de fuerza armada
     * @param parametrosCalculo parametros de cálculo
     * @return el factor correspondiente al conyuge que no trabaja
     * @since ? 
     */
    private float getFactorCnyNoTrabaja(int id, Constantes parametrosCalculo) {

        float factor = 1;
        Parametros[] lista = parametrosCalculo.getCnyNoTrabaja();
        int len = lista != null ? lista.length : 0;
        for (int i = 0; i < len; i++) {
            if (Integer.parseInt(lista[i].getTms_clv().trim()) == id)
                factor = lista[i].getTms_fct();
        }
        return factor;
    }

    /**
     * <p> Obtiene el factor de las boletas de honorarios  </p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>1.1  09/09/2008, Manuel Durán M. (Imagemaker IT): Se modifica interfaz, añadiendo los parametros de
     *          cálculo, debido al cambio de stateful a stateless. Además se agrega javadoc.</li>
     * </ul>
     * 
     * @param id el grado de fuerza armada
     * @param parametrosCalculo parametros de cálculo
     * @return el factor correspondiente a las boletas 
     * @since ? 
     */
    private float getFactorCV(String id, Constantes parametrosCalculo) {

        float factor = 1;
        Parametros[] listCV = parametrosCalculo.getCVBoleta();
        int len = listCV != null ? listCV.length : 0;
        for (int i = 0; i < len; i++) {
            StringBuffer sb = new StringBuffer();
            sb.append("************************************\n");
            sb.append("******** DATOS FACTOR CV **********\n");
            sb.append("**                               **\n");
            sb.append("** ID: [" + id + "]\n");
            sb.append("** LEN: [" + len + "]\n");
            sb.append("TMS_VAL: [" + listCV[i].getTms_val() + "], i=" + i + "\n");
            sb.append("FCT: [" + listCV[i].getTms_fct() + "], i=" + i + "]");
            if (listCV[i].getTms_val().trim().compareToIgnoreCase(id.trim()) == 0) {
                sb.append("** LO ENCONTRAMOS!\n");
                sb.append("********************\n");
                if (log.isDebugEnabled()) {
                    log.debug(sb.toString());
                }
                factor = listCV[i].getTms_fct();
                break;
            }
            sb.append("** NO LO ENCONTRAMOS\n");
            sb.append("********************\n");
            if (log.isDebugEnabled()) {
                log.debug(sb.toString());
            }
        }

        return factor;
    }

    /**
     * <p> Obtiene el factor de los meses de las Boletas de honorarios  </p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>1.1  09/09/2008, Manuel Durán M. (Imagemaker IT): Se modifica interfaz, añadiendo los parametros de
     *          cálculo, debido al cambio de stateful a stateless. Además se agrega javadoc.</li>
     * </ul>
     * 
     * @param id el grado de fuerza armada
     * @param parametrosCalculo parametros de cálculo
     * @return el factor correspondiente a los meses de las boletas
     * @since ? 
     */
    private float getFactorMesesBH(int id, Constantes parametrosCalculo) {

        float factor = 1;
        int limiteinferior = 0;
        int limitesuperior = 0;

        Parametros[] listBH = parametrosCalculo.getMesesBH();
        int len = listBH != null ? listBH.length : 0;
        for (int i = 0; i < len; i++) {
            limitesuperior = Integer.parseInt(listBH[i].getTms_val().trim());
            if (log.isDebugEnabled()) {
                log.debug("LIMITE INFERIOR: " + limiteinferior + " < " + id + " < LIMITE SUPERIOR: "
                        + limitesuperior);
            }
            if (limiteinferior < id && limitesuperior >= id) {
                factor = listBH[i].getTms_fct();
                if (log.isDebugEnabled()) {
                    log.debug("FACTOR MESES BH: " + factor);
                }
                limiteinferior = Integer.parseInt(listBH[i].getTms_val().trim());
                return factor;
            }
        }
        return factor;
    }

    /**
     * <p> Calcula y setea liquidaciones exigidas tanto a clientes como no clientes de acuerdo a:
     * última liquidación de sueldo si: Renta Fija >= 80 % de la Renta Total (Fija + Variable)
     * 3 últimas liquidaciones de sueldo si: 50% <= Renta Fija < 80 % de la Renta Total
     * 6 últimas liquidaciones de sueldo si: Variabilida de Renta Variable > % Variabilidad
     * </p>
     *
     * Registro de versiones:<ul>
     * <li>1.0 ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>2.0 23/11/2007, Manuel Durán M. (Imagemaker IT): Se modifican los periodos 
     *         exigidos a los no clientes, los que usaran la misma regla de los clientes
     *         del banco. Ademas se agrega la javadocumentación.</li>
     * <li>2.1 09/09/2008, Manuel Durán M. (Imagemaker IT): Se modifica interfaz, añadiendo el TO de cálculo 
     *         de renta, debido al cambio de stateful a stateless. Se elimina código comentado. </li>
     * <li>3.0 15/04/2014 Rodrigo Pino (SEnTRA): Se agregan validaciones respecto de las partidas variables
     *                        en el ingreso de las 3 últimas liquidaciones de sueldo.
     *                        Se ajustan las líneas de logueo según la Normativa Vigente.
     * </ul>
     *
     * @param rentaTO To de cálculo de renta
     * @return TO de cálculo de renta con periodos exigidos seteados  
     * @since ?
     */  
    private CalculoRentaTO CalculaPeriodosExigidos(CalculoRentaTO rentaTO) {

        LiquidacionDeSueldo[] lSueldo = rentaTO.getEvaluacionCliente().getLiquidacionesDeSueldo();

        int len = lSueldo != null ? lSueldo.length : 0;
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("[CalculaPeriodosExigidos] : Numero de Liquidaciones: " + len);
        }
        if (getLogger().isDebugEnabled()) {
            getLogger()
                    .debug("[CalculaPeriodosExigidos] :Tipo Cliente : "
                    + rentaTO.getEvaluacionCliente().isEsClienteBci());
        }

        switch (len) {
        case 0:
            getLogger().debug("[CalculaPeriodosExigidos] :Evaluo Exigencias con 0 Liquidacion");
            rentaTO.getEvaluacionCliente().setPeriodosExigidos(1);
            break;
        case 1:
            getLogger().debug("[CalculaPeriodosExigidos] :Evaluo Exigencias con 1 Liquidacion");
            if (lSueldo[0].getPorcentajeIngresosFijos() >= rentaTO.getParametrosCalculo().getPctFijoRLM()) {
                getLogger().debug("[CalculaPeriodosExigidos] :Aplica regla 1");
                rentaTO.getEvaluacionCliente().setPeriodosExigidos(1);
            } else {
                getLogger().debug("[CalculaPeriodosExigidos] :Aplica regla 3");
                rentaTO.getEvaluacionCliente().setPeriodosExigidos(3);
            }
            
            if(!rentaTO.getEvaluacionCliente().isEsClienteBci()){
                getLogger().debug("[[CalculaPeriodosExigidos] Cliente Bci Nuevo, ingresa 3 últimas liquidaciones.");
                 rentaTO.getEvaluacionCliente().setPeriodosExigidos(PERIODOS_EXIGIDOS);
             }

            if(rentaTO.getEvaluacionCliente().getLiquidacionesDeSueldo()[0].getIngresosVariables() > 0){
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("[[CalculaPeriodosExigidos] Liquidación Ingresada Posee Partidas Variables"
                                       +", ingresa 3 últimas liquidaciones.");
                }
                rentaTO.getEvaluacionCliente().setPeriodosExigidos(PERIODOS_EXIGIDOS);
             }

            break;
        case 3:
            getLogger().debug("[CalculaPeriodosExigidos] :Evaluo Exigencias con 3 Liquidacion");

            for(int i=0;i < lSueldo.length - 1; i++){
                if(lSueldo[i].getTotalIngresosVariables() != 0){
                    rentaTO.getEvaluacionCliente().setPeriodosExigidos(PERIODOS_EXIGIDOS);
                }
            }
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("[CalculaPeriodosExigidos] :getVariabilidadRentaVariable: "
                        + getVariabilidadRentaVariable(rentaTO.getEvaluacionCliente()));
            }
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("[CalculaPeriodosExigidos] :parametrosCalculo.getVariabilidad(): "
                        + rentaTO.getParametrosCalculo().getVariabilidad());
            }

            if (getVariabilidadRentaVariable(rentaTO.getEvaluacionCliente()) > rentaTO.getParametrosCalculo()
                    .getVariabilidad()) {
                getLogger().debug("[CalculaPeriodosExigidos] :Aplica regla 6");
                rentaTO.getEvaluacionCliente().setPeriodosExigidos(6);
            }
            break;
        }
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("[CalculaPeriodosExigidos] :****************Exigidos/Ingresados "
                    + rentaTO.getEvaluacionCliente().getPeriodosExigidos() + "/"
                    + rentaTO.getEvaluacionCliente().getPeriodosIngresados());
        }
        return rentaTO;
    }

    /**
     * <p> Obtiene la variabilidad de la renta variable</p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>1.1  09/09/2008, Manuel Durán M. (Imagemaker IT): Se modifica interfaz, añadiendo la evaluacion
     *          del cliente, debido al cambio de stateful a stateless. Se elimina código comentado, Además se 
     *          agrega javadoc.</li>
     * </ul>
     * 
     * @param evaluacionCliente Evaluacion de renta del cliente  * 
     * @return la variabilidad de la renta variable 
     * @since ? 
     */
    private float getVariabilidadRentaVariable(RentaCliente evaluacionCliente) {
        LiquidacionDeSueldo[] lSueldo = evaluacionCliente.getLiquidacionesDeSueldo();

        int len = lSueldo != null ? lSueldo.length : 0;

        if (len == 0) {
            // No entra por aca.
            return 0;
        }

        float variabilidades[] = new float[len];
        /**/
        for (int i = 0; i < len; i++) {
            float totalDescuentos = 0;
            totalDescuentos = lSueldo[i].getTotalDescuentos() < 0 ? lSueldo[i].getTotalDescuentos() * -1
                    : lSueldo[i].getTotalDescuentos();
            if (log.isDebugEnabled()) {
                log.debug("OJO Variabilidad Antes: " + variabilidades[i]);
            }
            variabilidades[i] = lSueldo[i].getTotalRLMajustada();
            if (log.isDebugEnabled()) {
                log.debug("OJO Variabilidad Despues: " + variabilidades[i]);
            }
            if (log.isDebugEnabled()) {
                log.debug("Renta Variable: " + lSueldo[i].getIngresosVariables());
            }
            if (log.isDebugEnabled()) {
                log.debug("LSUELDO[" + lSueldo[i]);
            }
            if (log.isDebugEnabled()) {
                log.debug("Total Descuentos: " + lSueldo[i].getTotalDescuentos());
            }
            if (log.isDebugEnabled()) {
                log.debug(" (%) Ingresos variables: " + lSueldo[i].getSubTotalIngresoVariable());
            }
        }
        log.debug("Determinando maximos y minimos de variacion");
        Arrays.sort(variabilidades);
        float variabilidadMaxima = Math.max(variabilidades[len - 1], variabilidades[0]);
        float variabilidadMin = Math.min(variabilidades[len - 1], variabilidades[0]);
        if (log.isDebugEnabled()) {
            log.debug("Variabilidades Maxima: " + variabilidadMaxima + ", Minima: " + variabilidadMin);
        }

        float variabilidad = (variabilidadMaxima - variabilidadMin) / variabilidadMin;
        if (log.isDebugEnabled()) {
            log.debug("OJO Variabilidad Final: " + variabilidad);
        }

        return variabilidad;
    }

    /**
     * Método que setea los datos del cliente en el VO RentaCliente
     * <p>
     * @param rCliente renta del cliente sobre la cual se desea realizar los calculos
     * <p>
     * Registro de versiones:<ul>
     * <li>1.0 sin fecha Incorporacion - sin autor - versión inicial
     * <li>1.1 10-11-2005 - Emilio Fuentealba Silva (SEnTRA)- se agrega el
     * dato del canal por donde se realizan las modificaciones
     * <li>1.2 09/09/2008, Manuel Durán M. (Imagemaker IT): Se depreca ya que no es utilizado y datos se setean
     *         a nivel de servlets.</li>
     * </ul>
     * <p>
     * @since 1.0
     * @deprecated No debe ser utilizado, se debe setear a nivel de servlets
     */
    private void configuraPeriodoLiquidaciones(RentaCliente rCliente, int totalLiquidaciones) throws CalculoRentaException {

    }

    /**
     * Método que setea los datos del cliente en el VO RentaCliente
     * <p>
     * @param rCliente renta del cliente sobre la cual se desea realizar los calculos
     * <p>
     * Registro de versiones:<ul>
     * <li>1.0 sin fecha Incorporacion - sin autor - versión inicial
     * <li>1.1 10-11-2005 - Emilio Fuentealba Silva (SEnTRA)- se agrega el
     * dato del canal por donde se realizan las modificaciones
     * <li>1.2 09/09/2008, Manuel Durán M. (Imagemaker IT): Se depreca debido a cambios de stateful a stateless, ya que 
     *         se deberan setear sobre el TO desde servlet de donde es llamado .</li>
     * </ul>
     * <p>
     * @since 1.0
     * @deprecated No debe ser utilizado, se debe setear a nivel de servlets
     */
    private String setPeriodoLiquidacion(RentaCliente evaluacionCliente) {
        return null;
    }

    private String getPeriodoActualLiquidacion() {
        return getFechaPeriodo(null, -1);
    }

    private String getPeriodoActualLiquidacion2() {
        return getFechaPeriodo(null, -2);
    }

    private String getProximoPeriodoLiquidacion(String ultimoPeriodo) {
        return getFechaPeriodo(ultimoPeriodo, -1);
    }

    /**
     * <p> genera un rango de fechas, donde la fecha inicial corresponde 
     *     a la fecha actual más los meses indicados en el parámetro largo, 
     *     y la fecha final corresponde a la fecha actual.
     * </p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>1.0a 26/06/2007, Manuel Durán M. (Imagemaker IT): Se agrega la javadocumentación.</li>
     * </ul>
     *
     * @param largo Cantidad de meses a sumar para generar la fecha inicial
     * @return un rango de fecha con formato AAAAMMAAAAMM  
     * @since ?
     */         
    private String getRangoPeriodo(int largo) {
        String rango = getFechaPeriodo(null, largo) + "" + getFechaPeriodo(null, 0);
        return rango;
    }

    /**
     * <p> sobrecarga del método getRangoPeriodo(int), el cual genera un rango de fechas tomando la fecha inicial
     *     desde la tabla de parametros "CalculoRenta.parametros". La fecha final corresponde a la fecha actual. 
     *     </p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  26/06/2007, Manuel Durán M. (Imagemaker IT): Versión inicial.</li>
     * </ul>
     *
     * @return un rango de fecha con formato AAAAMMAAAAMM 
     * @since 4.1 
     */    
    private String getRangoPeriodo() {
        // Obtiene el parametro RangoFecha inicial que contiene la fecha más
        // antigua de las liquidaciones almacenadas
        String periodoInicial = TablaValores.getValor(parametrosNombreArchivo, "RangoFechaInicial", "valor");
        String rango = periodoInicial + "" + getFechaPeriodo(null, 0);
        return rango;
    }


    private String getRangoPeriodoDespues15(int largo) {
        String rango = getFechaPeriodo(getFechaPeriodo(-1), largo) + "" + getFechaPeriodo(getFechaPeriodo(-1), 0);
        if (log.isDebugEnabled()) { log.debug("OJO rango de liquidaciones despues del 15: " + rango); }
        return rango;
    }

    private String getFechaPeriodo(int largo) {
        String periodo;
        Calendar rightNow = Calendar.getInstance();
        rightNow.setTime(new Date());
        rightNow.add(Calendar.MONTH, -largo);
        if ((rightNow.get(Calendar.MONTH) + 1) < 10)
            periodo = (rightNow.get(Calendar.YEAR)) + "0" + (rightNow.get(Calendar.MONTH) + 1);
        else
            periodo = (rightNow.get(Calendar.YEAR)) + "" + (rightNow.get(Calendar.MONTH) + 1);

        return periodo;
    }

    private String getFechaPeriodo(String ultimoPeriodo, int saltoPeriodo) {

        String proximoPeriodo = null;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMM");
        Date fechaPeriodo = null;

        try {
            if (ultimoPeriodo == null || ultimoPeriodo.trim().equals(""))
                fechaPeriodo = new Date();
            else
                fechaPeriodo = sdf.parse(ultimoPeriodo);
        } catch (ParseException e1) {
            e1.printStackTrace();
        }

        Calendar rightNow = Calendar.getInstance();
        rightNow.setTime(fechaPeriodo);

        rightNow.add(Calendar.MONTH, saltoPeriodo);
        if ((rightNow.get(Calendar.MONTH) + 1) < 10)
            proximoPeriodo = (rightNow.get(Calendar.YEAR)) + "0" + (rightNow.get(Calendar.MONTH) + 1);
        else
            proximoPeriodo = (rightNow.get(Calendar.YEAR)) + "" + (rightNow.get(Calendar.MONTH) + 1);

        if (log.isDebugEnabled()) { log.debug("FECHA PERIODO: " + proximoPeriodo); }
        return proximoPeriodo;

    }


    private int getSaltoPeriodoDai()
    {
        /**
         * Si el mes actual esta entre Mayo y Diciembre (Inclusives)
         * entonces el salto sera 0 (Mismo año)
         *
         * Caso contrario será el año anterior (salto = -1)
         */
        Calendar fechaActual = Calendar.getInstance();
        if (fechaActual.get(Calendar.MONTH) >= Calendar.MAY && fechaActual.get(Calendar.MONTH) <= Calendar.DECEMBER) {
            return 0;
        }

        return -1;
    }

    private String getPeriodoDai(int periodo) {
        return getFechaPeriodoDai(null, periodo);
    }

    private String getFechaPeriodoDai(String ultimoPeriodo, int saltoPeriodo) {

        String proximoPeriodo = null;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
        Date fechaPeriodo = new Date();

        Calendar rightNow = Calendar.getInstance();
        rightNow.setTime(fechaPeriodo);

        rightNow.add(Calendar.YEAR, saltoPeriodo);
        return String.valueOf(rightNow.get(Calendar.YEAR));
    }

    private String getGlosaPeriodo(String periodo) throws CalculoRentaException {

        String glosa = null;

        if (log.isDebugEnabled()) { log.debug("SETEO LA GLOSA PARA EL PERIODO : " + periodo); }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMM");
        Date fechaPeriodo = null;

        try {
            fechaPeriodo = sdf.parse(periodo != null && !periodo.trim().equals("")? periodo : "197001");
        } catch (ParseException e1) {
            if( log.isEnabledFor(Level.ERROR) ){
                log.error("[getGlosaPeriodo] Ha ocurrido un error al obtener la fecha del periodo: "+ e1);
            }
        }

        Calendar rightNow = Calendar.getInstance();
        rightNow.setTime(fechaPeriodo);

        if (log.isDebugEnabled()) { log.debug("EL MES ES : " + rightNow.get(Calendar.MONTH) + 1); }

        switch (rightNow.get(Calendar.MONTH) + 1) {
        case 1 :
            glosa = "Enero  de " + (rightNow.get(Calendar.YEAR));
            break;
        case 2 :
            glosa = "Febrero de " + (rightNow.get(Calendar.YEAR));
            break;
        case 3 :
            glosa = "Marzo  de " + (rightNow.get(Calendar.YEAR));
            break;
        case 4 :
            glosa = "Abril   de " + (rightNow.get(Calendar.YEAR));
            break;
        case 5 :
            glosa = "Mayo    de " + (rightNow.get(Calendar.YEAR));
            break;
        case 6 :
            glosa = "Junio   de " + (rightNow.get(Calendar.YEAR));
            break;
        case 7 :
            glosa = "Julio   de " + (rightNow.get(Calendar.YEAR));
            break;
        case 8 :
            glosa = "Agosto  de " + (rightNow.get(Calendar.YEAR));
            break;
        case 9 :
            glosa = "Septiembre de " + (rightNow.get(Calendar.YEAR));
            break;
        case 10 :
            glosa = "Octubre de " + (rightNow.get(Calendar.YEAR));
            break;
        case 11 :
            glosa = "Noviembre de " + (rightNow.get(Calendar.YEAR));
            break;
        case 12 :
            glosa = "Diciembre de " + (rightNow.get(Calendar.YEAR));
            break;
        }

        if (log.isDebugEnabled()) { log.debug("FECHA PERIODO: " + glosa); }

        return glosa;

    }

    private Date getFecha(String fecha, String fmt) {
        SimpleDateFormat sdf = new SimpleDateFormat(fmt);
        try {
            return sdf.parse(fecha);
        } catch (ParseException e) {
            if( log.isEnabledFor(Level.ERROR) ){
                log.error("[getFehca] Ha ocurrido un error al obtener la fecha: " + e);
            }
            return null;
        } catch (Exception e) {
            if( log.isEnabledFor(Level.ERROR) ){
                log.error("[getFehca] Ha ocurrido un error al obtener la fecha: " + e);
            }
            return null;
        }
    }


    /**
     * <p> Envío de datos para modificación en Sistema General de Clientes. </p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  ??/??/????, ??????????. (????????): Versión inicial.</li>
     * <li>2.0  15/05/2006 Nelly Reyes Quezada, SEnTRA: Se agrega validación de blanco para formateo de renta fija a miles de pesos.
     * <li>2.1  26/06/2007, Manuel Durán M. (Imagemaker IT): Se sobreescribe la renta del conyuge que provee
     *          ConsCliPerIBM del cliente por la suma de rentaVariabla y rentaFija, obtenidas con el rut 
     *          del conyuge desde ConsCliPerIBM. Se agrega comprobación que rut del conyuge no sea vacío 
     *          para no consultar al servicio cuando no tenga conyuge asociado. Además se modifica 
     *          Javadocumentación.</li>
     * <li>2.2  22/04/2009, Pedro Carmona Escobar (SEnTRA): Se depreca debido a cambios de stateful a stateless. En su reemplazo debe utilizarse
     *          el método {@link #actualizaRentasCliente(long, char, RentaCliente)}.</li>
     * </ul>
     *
     * @param rutcliente rut del cliente
     * @param dvCliente dígiro verificador del cliente
     * @param rentaCliente Renta del cliente a modificar en el Sistema General de Clientes
     * @return verdadero si logró modificar la renta del cliente
     * @throws wcorp.util.GeneralException
     * @throws wcorp.util.com.TuxedoException
     * @throws wcorp.serv.clientes.ClientesException
     * @throws java.rmi.RemoteException
     * @throws javax.naming.NamingException
     * @throws javax.ejb.CreateException
     * 
     * @since ?
     * @deprecated Se debe utilizar nuevo método de grabación actualizaRentasClientes
     */  

    private boolean modificaRentasCliente(long rutcliente, char dvCliente, RentaCliente rentaCliente)
            throws GeneralException,TuxedoException, ClientesException, RemoteException,NamingException, CreateException{
        return false;
    }




    /**
     * <p>  Este metodo permite rescatar la boleta de hononarios con el menor monto  </p>
     *
     * Registro de versiones:<ul>
     * <li>1.0  15/11/2004, Alexander Díaz R . (Apolo Ingeniería): Versión inicial.</li>
     * <li>1.1  09/09/2008, Manuel Durán M. (Imagemaker IT): Se modifica interfaz, la evaluacion del
     *          cliente, debido al cambio de stateful a stateless. Además se agrega javadoc.</li>
     * </ul>
     * 
     * @param evaluacionCliente Evaluacion de renta del cliente
     * @return el monto de la menor boleta de honorarios
     * @since ? 
     */  
    private String getMontoBoletaMenor(RentaCliente evaluacionCliente){

        BoletaHonorarios[] boletas = evaluacionCliente.getBoletasHonorarios();
        float fltMenorBoleta=0;
        boolean menorActual=false;
        boolean menorSgte=false;
        int numeroBoletas = boletas != null ? boletas.length : 0;
        if (numeroBoletas > 0) {
            log.debug("[getMontoboletaMenor]numero boletas>0");
            evaluacionCliente.setPeriodoBol(boletas[0].getPeriodoBoleta() + boletas[numeroBoletas - 1].getPeriodoBoleta());
            log.debug("[getMontoboletaMenor]Iterando Boletas");
            for (int x=0;x<numeroBoletas;x++){
                if (log.isDebugEnabled()) { log.debug("[getMontoboletaMenor]objBoleta.periodo   : "+boletas[x].getPeriodoBoleta()); }
                if (log.isDebugEnabled()) { log.debug("[getMontoboletaMenor]objBoleta.monto : "+boletas[x].getMontoBoleta()); }
                try{
                    if (x+1<numeroBoletas){
                        if (boletas[x].getMontoBoleta()<boletas[x+1].getMontoBoleta()){
                            menorActual=true;
                        }else{menorSgte=true;}

                        if (x==0 && menorActual){
                            fltMenorBoleta=boletas[x].getMontoBoleta();
                        }
                        if (x>0 && menorActual && boletas[x].getMontoBoleta()<fltMenorBoleta){
                            fltMenorBoleta=boletas[x].getMontoBoleta();
                        }
                        if (x==0 && menorSgte){
                            fltMenorBoleta=boletas[x+1].getMontoBoleta();
                        }
                        if (x>0 && menorSgte && boletas[x+1].getMontoBoleta()<fltMenorBoleta){
                            fltMenorBoleta=boletas[x+1].getMontoBoleta();
                        }
                    }
                }catch(Exception ex){
                    if( log.isEnabledFor(Level.ERROR) ){
                        log.error("[getMontoboletaMenor] Excepcion: " + ex);
                    }
                }
            }
        }

        if (log.isDebugEnabled()) { log.debug("[getMontoboletaMenor]Menor Boleta Final  : "+fltMenorBoleta); }
        return String.valueOf(fltMenorBoleta);
    }
    /**
     * Invoca a los servicios que guarda la informacion de la renta fija
     * con los ingresos adicionales del cliente como lo son
     * arriendo de bienes raíces, pension o jubilacion y pension alimenticia.
     * Guarda el canal por el cual se estan introduciendo los cambios
     * <p>
     * @param rentaFija de la renta del cliente.
     * @param canal por donde se estan realizando los cambios
     * <p>
     * Registro de versiones:<ul>
     * <li>1.0 21/10/2005 - Emilio Fuentealba Silva - versión inicial
     * <li>1.1 09/09/2008, Manuel Durán M. (Imagemaker IT): Se depreca debido a cambios de stateful a 
     *         stateless, ya que se deberan setear sobre el TO desde servlet de donde es llamado .</li>
     * </ul>
     * <p>
     * @exception wcorp.util.com.TuxedoException
     * @exception wcorp.util.CalculoRentaException
     * @since 2.0
     * @deprecated Valores deben setearse directamente sobre TO 
     */
    public void setRentaFija(float rentaFija,String canal) throws CalculoRentaException {

    }

    public void ejbCreate() throws javax.ejb.CreateException {
        try {
            /*
                // Obtain the EJB home
                Properties env = new Properties();
                env.put( "java.naming.factory.initial", "desisoft.ejb.client.JRMPFactory" );
                env.put( "desisoft.ejb.nameServer1", "localhost:2050" );
                Context ctx = new InitialContext( env );
                log.debug("Intento Obtener Bean ServicioConsultaRenta");
                this.trxHome = (ServicioCalculoRentaHome) PortableRemoteObject.narrow( ctx.lookup( "ServicioCalculoRenta" ), ServicioCalculoRentaHome.class );
                this.trxBean = trxHome.create();
             */
            InitialContext ctx = JNDIConfig.getInitialContext();
            this.trxHome = (ServicioCalculoRentaHome) ctx.lookup("wcorp.serv.renta.ServicioCalculoRenta");
            this.trxBean = trxHome.create();

            EnhancedServiceLocator locator = EnhancedServiceLocator.getInstance();
            ServiciosEconomiaHome economiaHome = (ServiciosEconomiaHome)locator.getHome("wcorp.serv.economia.ServiciosEconomia", ServiciosEconomiaHome.class);
            servEconomia = economiaHome.create();
            log.debug("[ejbCreate]Instancia servEconomia: ["+ servEconomia+"]");            


        } catch (Exception ex) {
            if( log.isEnabledFor(Level.ERROR) ){ 
                log.error("No puedo obtener el Bean  ServicioConsultasSegurosCia: " + ex.getMessage()); 
            }
            ex.printStackTrace();
        }
    }
    /**
     * Invoca al método en los servicios que guarda la informacion
     * de los ingresos adicionales (arriendo de bienes raíces,
     * pension o jubilacion y pension alimenticia.) de forma persistente
     *
     * <p>
     * @param rut mantiza del rut del cliente
     * @param dv digito verificador del cliente
     * @param numeroSecuencia numero de secuencia que relaciona el ingreso
     * @param montoArriendoBienesRaices monto en arriendo bienes raices
     * @param montoPensionJubilacion monto en pension jubilacion
     * @param montoPensionAlimenticia monto en pension alimenticia
     * @param usuarioIngresa ejecutivo que realiza el ingreso
     * @param periodo perido del ingreso adicional
     * <p>
     * Registro de versiones:<ul>
     * <li>1.0 21/10/2005 - Emilio Fuentealba Silva - versión inicial
     * </ul>
     * <p>
     * @exception wcorp.util.com.TuxedoException
     * @exception wcorp.util.CalculoRentaException
     * @since 2.0
     */
    public int setIngresosAdicionales(String rut, String dv,
            int numeroSecuencia,
            float montoArriendoBienesRaices,
            float montoPensionJubilacion,
            float montoPensionAlimenticia,
            String usuarioIngresa,
            String periodo)
                    throws CalculoRentaException {

        log.debug("***** se incorporan los ingresos adicionales ******");
        int rqSrvArriendo=0;
        int rqSrvJubilacion=0;
        int rqSrvAlimenticia=0;
        int respuestaServicio=0;
        try {
            if(montoArriendoBienesRaices>0){
                rqSrvArriendo = trxBean.ingresaIngresosAdicionales(
                        rut, dv,
                        numeroSecuencia, montoArriendoBienesRaices,
                        "001", usuarioIngresa,periodo);
                if (log.isDebugEnabled()) { log.debug(
                        "ESTA ES LA RESPUESTA EN ARRIENDO BIENES RAICES: " + rqSrvArriendo); }
            }
            if(montoPensionJubilacion>0){
                rqSrvJubilacion = trxBean.ingresaIngresosAdicionales(
                        rut, dv,
                        numeroSecuencia, montoPensionJubilacion,
                        "002", usuarioIngresa,periodo);
                if (log.isDebugEnabled()) { log.debug(
                        "ESTA ES LA RESPUESTA EN PENSION JUBILACION: " + rqSrvJubilacion); }

            }
            if(montoPensionAlimenticia>0){
                rqSrvAlimenticia = trxBean.ingresaIngresosAdicionales(
                        rut, dv,
                        numeroSecuencia, montoPensionAlimenticia,
                        "003", usuarioIngresa,periodo);
                if (log.isDebugEnabled()) { log.debug(
                        "ESTA ES LA RESPUESTA EN PENSION ALIMENTICIA: " + rqSrvAlimenticia); }

            }


            if(rqSrvArriendo!=0||rqSrvJubilacion!=0||rqSrvAlimenticia!=0){
                log.debug("Problemas (bprocess.CalculoRenta.setIngresosAdicionales) No se han podido ingresar los ingresos");
                respuestaServicio=-1;
                throw new CalculoRentaException("INSERCION_INGRESOS_ADICIONALES_FALLO");
            }
            else
            {
                log.debug("setIngresosAdicionales:: Los datos han sido ingresados de forma correcta");
                respuestaServicio=0;
            }
        } catch (Exception ex) {
            if (log.isEnabledFor(Level.ERROR)) {
                log.error("Problemas (bprocess.CalculoRenta.setBoletaHonorarios) " + ex.getMessage());
            }
            throw new CalculoRentaException(ex.getMessage());
        }


        return respuestaServicio;
    }
    /**
     * Método de paso antes de llamar al método que obtiene los ingresos del cliente
     * y modifica el VO por referencia.
     * Registro de versiones:<ul>
     * <li>1.0 10/11/2005 - Emilio Fuentealba Silva - versión inicial
     * <li>1.1 09/09/2008, Manuel Durán M. (Imagemaker IT): Se modifica interfaz, ñadiendo el TO de cálculo 
     *         de renta, debido al cambio de stateful a stateless. Además se agrega javadoc.</li>
     * </ul>
     * <p>
     * 
     * @param rentaTO TO de cálculo de renta
     * @exception wcorp.util.com.TuxedoException
     * @since 2.0
     */
    private void cargaIngresosAdicionales(CalculoRentaTO rentaTO) throws CalculoRentaException,
    TuxedoException, RemoteException {
        cargaIngresosAdicionales(rentaTO.getEvaluacionCliente(), false);
    }
    /**
     * <p> Método que recupera la información acerca de los ingresos adicionales
     *     (Arriendo bienes raices, pensión jubilación, pensión alimenticia)
     *     Este método modifica los atributos del objeto de tipo RentaCliente</p>
     * 
     * Registro de versiones:<ul>
     * <li>1.0 10/11/2005 - Emilio Fuentealba Silva - versión inicial</li>
     * <li>1.1 26/06/2007, Manuel Durán M. (Imagemaker IT): Se elimina periodo de en duro 2005 por la llamada al
     * método getRangoPeriodo obteniendo solo el año de la fecha inicial, esto para mantener consistencia en 
     * los métodos que obtienen los historicos. Además se corrige el formato de la javadocumentacion.
     * </ul>
     * 
     * @param evaluacion evaluacion actual del cliente
     * @param historico indica si se esta rescatando el ingreso adicional de forma historica
     * @throws wcorp.util.com.TuxedoException
     * @throws wcorp.serv.renta.CalculoRentaException
     * @throws java.rmi.RemoteException
     * @since ?
     */         
    private void cargaIngresosAdicionales(RentaCliente evaluacion, boolean historico)
            throws CalculoRentaException, TuxedoException, RemoteException
            {
        log.debug("bprocess.CalculoRenta.cargaIngresosAdicionales");


        String rutCliente = evaluacion.getRutCliente();
        String dvRut = evaluacion.getDvRut();
        int numeroSecuencia = evaluacion.getNumeroSecuencia();


        String periodoConsulta = getRangoPeriodo().substring(0,4);

        log.debug("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        if (log.isDebugEnabled()) { log.debug("obtieneIngresosAdicionales:: Rut cliente["+ rutCliente+"]"); }
        if (log.isDebugEnabled()) { log.debug("obtieneIngresosAdicionales:: Dv cliente["+ dvRut+"]"); }
        if (log.isDebugEnabled()) { log.debug("obtieneIngresosAdicionales:: Numero secuencia["+ numeroSecuencia+"]"); }
        if (log.isDebugEnabled()) { log.debug("obtieneIngresosAdicionales:: Periodo Ingreso["+ periodoConsulta+"]"); }
        log.debug("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");

        if (log.isDebugEnabled()) { log.debug("El rango del periodo es : " + periodoConsulta); }
        float[] ingresos=trxBean.obtieneIngresosAdicionales(rutCliente, dvRut, numeroSecuencia, periodoConsulta);

        if (ingresos != null) {
            evaluacion.setArriendoBienesRaices(ingresos[0]);
            evaluacion.setPensionJubilacion(ingresos[1]);
            evaluacion.setPensionAlimenticia(ingresos[2]);
        }else{
            evaluacion.setArriendoBienesRaices(0);
            evaluacion.setPensionJubilacion(0);
            evaluacion.setPensionAlimenticia(0);
        }
            }



    // TBD:  If any other ejbCreate's are added manually to the home interface, define them.
    //

    // Other methods required in a session bean
    //

    /**
     * Método que hace un rollback de los datos históricos cuando el intento de
     * actualización de los datos en el sistema general(SGC) de clientes falla (IBM).
     *  Si el almacenado en el SGC se ejecuta correctamente, este método borra la información
     *  temporal que queda antes del rollback.
     * <p>
     * @param rut del cliente
     * @param dv verificador del cliente
     * @param numeroSecuencia , valor que permite buscar el registro exacto
     * @param indicador de estatus, con valores: 1 (IBM falló) ó 0 (IBM OK)
     * <p>
     * Registro de versiones:<ul>
     *   <li>1.0 27/02/2007  Osvaldo Lara Lira(SEnTRA) :  versión inicial</li>
     *   <li>1.1 02/10/2009  Eugenio Contreras (Imagemaker IT): Se corrige y agregan líneas de log</li>
     *   <li>1.2 25/05/2010, Roberto Rodríguez(Imagemaker IT): Se agregan líneas de log con el fin  de 
     *   sacarlos a un archivo diferente </li>   
     *    <li> 1.3 14/07/2010, Roberto Rodríguez(Imagemaker IT): Se agrega más información en los logs </li>            
     *   <li> 1.4 19/03/2015 Alejandro Barra (SEnTRA) - Andrés Alvarado (Ing. Soft. BCI): se elimina variable logCalculo y se agrega llamado a método getLogger.
     * </li>
     * </ul>
     * <p>
     * @since 4.0
     */
    private void actualizaDatosHistoricos(String rut, String dv, int numeroSecuencia,int indicador){
        if (getLogger().isEnabledFor(Level.INFO)){
            getLogger().info("[actualizaDatosHistoricos][" + rut + "][BCI_INI] inicio dv[" + dv + "]"
                + " numeroSecuencia[" + numeroSecuencia + "] indicador[" + indicador + "]");
        }
        if (getLogger().isEnabledFor(Level.DEBUG)){
            getLogger().debug("[actualizaDatosHistoricos] Parámetros recibidos");
            getLogger().debug("[actualizaDatosHistoricos] Rut cliente["+ rut+"]");
            getLogger().debug("[actualizaDatosHistoricos] Dv cliente["+ dv+"]");
            getLogger().debug("[actualizaDatosHistoricos] Numero secuencia["+ numeroSecuencia+"]");
            getLogger().debug("[actualizaDatosHistoricos] indicador["+ indicador+"]");
        }

        try{
            if (getLogger().isEnabledFor(Level.DEBUG)){
                getLogger().debug("[actualizaDatosHistoricos] Se llama a método: "
                    + "ServicioCalculoRentaBean#actualizaDatosHistoricos");
            }
            trxBean.actualizaDatosHistoricos(rut, dv, numeroSecuencia, indicador);
            if (getLogger().isEnabledFor(Level.DEBUG)){
                getLogger().debug("[actualizaDatosHistoricos] El método: "
                    + "ServicioCalculoRentaBean#actualizaDatosHistoricos, se ejecuta correctamente");
            }
        }
        catch(TuxedoException e){
            if(getLogger().isEnabledFor(Level.ERROR)){
                getLogger().error("[actualizaDatosHistoricos][TuxedoException] error"
                + " con mensaje: " + e.getMessage(), e);
            }
        }
        catch(RemoteException e){
            if(getLogger().isEnabledFor(Level.ERROR)){
                getLogger().error("[actualizaDatosHistoricos][RemoteException] error"
                + " con mensaje: " + e.getMessage(), e);
            }
            }
        if(getLogger().isEnabledFor(Level.INFO)){
            getLogger().info("[actualizaDatosHistoricos][BCI_FINOK]");
        }
    }


    /**
     * <p>
     * Envío de datos para modificación en Sistema General de Clientes.
     * </p>
     * Registro de versiones:
     * <ul>
     * <li> 1.0 23/11/2007 Manuel Durán M. (Imagemaker IT): Versión inicial</li>
     * <li> 1.1 22/04/2009 Pedro Carmona Escobar (SEnTRA): Se reemplaza la lógica de obtención de la instancia del
     * servicios Cliente por la llamada método {@link #obtenerInstanciaSeriviciosCliente()}.</li>
     * <li> 1.2 02/10/2009 Eugenio Contreras (Imagemaker IT): Se modifican y agregan líneas de log.</li>
     * <li> 1.3 25/02/2010 Eugenio Contreras (Imagemaker IT): Se agrega validación que permite determinar cuando el
     * origen de la renta es por DAI y de esta manera asegurar la conversión de la renta fija del formato de pesos
     * a miles.</li>
     * <li> 1.4 09/09/2010 Miguel Sepúlveda (Imagemaker IT): Se cambia logger de "log" a "logCalculo", esto se
     * realiza para separar las lineas de log del método en archivo distinto definido para cálculo de renta.
     * <li> 1.5 21/03/2011 Miguel Sepúlveda G. (Imagemaker IT): Se agrega atributo codMov obtenido desde el objeto
     * de entrada <code>RentaCliente</code> esto para realizar el llamado al metodo sobrecargado de
     * {@link ServiciosCliente#actualizaClientePersonaRenta(Long, String, String, String, Double, Date, Double,
     * Date, String)}
     * <li>1.6 25/07/2011 Cristoffer Morales L.(TINet): Se cambia el modificador de acceso del método de private a
     * public para que pueda ser utilizado para actualizar las rentas de un cliente. No se modificó código interno
     * del método. Se realizó un formateo completo del método para evitar problemas de Checkstyle debido a
     * tabulación incorrecta. Se completó y formateo para corregir los problemas de formato en la Javadoc.
     *  <li> 1.7  16/02/2011, Christopher Finch U. (Imagemaker IT): Se agrega un trim() en la comparación del periodo DAI ya que para los casos en que
     *                                                          el periodo es del tipo liquidación de sueldo, el periodo DAI viene con un espacio. Esto hacía que el monto de renta fija
     *                                                          se dividiera por mil, produciendo una inconsistencia del valor a ser grabado (si el monto incial es menor de mil, el monto 
     *                                                          ingresado será cero). Con el trim se corrige el hecho que entre a esta parte del código, solucionando el problema de la
     *                                                          división por mil.</li>
     *                                                          
     * <li> 1.8 16/09/2014 Pedro Carmona Escobar. (SEnTRA): Se incorpora código para calcular la renta fija
     *                          a enviar al SGC de una forma particular para el caso en que el origen de la
     *                          solicitud sea "AUTOMATICO".Además, se retira el uso de la clase deprecada
     *                          Formatting. Por último, se ajusta a normativa el logueo del método.</li>
     *  <li> 1.9  29/09/2014 Pedro Rebolledo Lagno (SEnTRA): Se agrega la lógica para modificar la variable
     *                                                       "periodoLiqVar" para cuando el origen es "DAI".</li>
     * <li> 2.0 19/03/2015 Alejandro Barra (SEnTRA) - Andrés Alvarado (Ing. Soft. BCI): se elimina variable logCalculo y se agrega llamado a método getLogger.
     * </li>
     * <li> 2.1 20/07/2015 Jorge San Martín (SEnTRA) - Paula León (Ing. Soft. BCI): Se modifica para llamada al método {@link ServiciosClientes#actualizaClientePersonaRenta() 
     * con el código de usuario.
     * </li>
     * </ul>
     * 
     * @param rutcliente rut del cliente
     * @param dvCliente dígiro verificador del cliente
     * @param rentaCliente Renta del cliente a modificar en el Sistema General de Clientes
     * @return verdadero si logró modificar la renta del cliente
     * @throws GeneralException lanzada en caso de ocurrir un error de sistema.
     * @throws TuxedoException lanzada en caso de ocurrir un error de tuxedo.
     * @throws ClientesException lanzada en caso de ocurrir en un error en el proceso de cliente.
     * @throws RemoteException problema de comunicación con un servicio remoto.
     * @throws NamingException problema en la busqueda de un servicio.
     * @throws CreateException problema en la creación de un EJB.
     * @since 4.3
     */
    public boolean actualizaRentasCliente(long rutcliente, char dvCliente, RentaCliente rentaCliente)
            throws GeneralException, TuxedoException, ClientesException, RemoteException, NamingException,
            CreateException {
        if (getLogger().isEnabledFor(Level.INFO)){
            getLogger().info("[actualizaRentasCliente]["+rutcliente+"][BCI_INI] dvCliente:" + dvCliente
                    + "    Renta: " + rentaCliente);
        }

        try {
            if (getLogger().isEnabledFor(Level.DEBUG)){
                getLogger().debug("[actualizaRentasCliente]["+rutcliente
                        +"]Se obtiene instancia de EJB ServiciosClienteBean");
            }
            ServiciosCliente srvCliente = obtenerInstanciaSeriviciosCliente();
            if (getLogger().isEnabledFor(Level.DEBUG)){
                getLogger().debug("[actualizaRentasCliente]["+rutcliente
                        +"]Se verifica que cliente sea cliente persona");
            }
            ClientePersona persona = srvCliente.getClientePersonaTodo(rutcliente, dvCliente);
            if (getLogger().isEnabledFor(Level.DEBUG)){
                getLogger().debug("[actualizaRentasCliente]["+rutcliente
                        +"] persona: " + persona);
            }

            wcorp.model.actores.cliente.Conyuge conyuge = (wcorp.model.actores.cliente.Conyuge)
                    persona.antecFamiliar.get("conyuge");

            String strdv = "";
            if (!conyuge.rut.trim().equals("") && conyuge.rut != null) {
                strdv = conyuge.dv + "";
            }
            Date periodoLiqVar = rentaCliente.getFechaActualiza();
            if (periodoLiqVar == null)
                periodoLiqVar = new Date();
            
            DecimalFormat df = new DecimalFormat("###0");
            DecimalFormatSymbols dfs  = new DecimalFormatSymbols(new Locale("es", "CL"));
            df.setDecimalFormatSymbols(dfs);
            
            Double rv = new Double(new Float(df.format(rentaCliente.getRentaVariable() / FACTOR_MILES))
                .doubleValue());

            Double rl = null;
            if (getLogger().isEnabledFor(Level.DEBUG)) {
                getLogger().debug("[actualizaRentasCliente] rentaCliente.getCodOrigen() = {"
                    + rentaCliente.getCodOrigen() + "}");
            }
            if (rentaCliente.getOrigenCalculo()!=null && (rentaCliente.getOrigenCalculo()
                    .equalsIgnoreCase("AUTOMATICO"))){
                rl =new Double(rentaCliente.getRentaFija()/FACTOR_MILES);
            }
            else{
                if (rentaCliente.getPeriodoLiq() == null || rentaCliente.getPeriodoLiq().trim().equals(""))
                    rl = new Double(new Float(df.format(rentaCliente.getRentaFija() / FACTOR_MILES))
                    .doubleValue());
                else
                    rl = new Double(new Float(df.format(rentaCliente.getRentaFija())).doubleValue());
                
                if( rentaCliente.getPeriodoDai()!=null && !rentaCliente.getPeriodoDai().trim().equals("") ){
                    rl = new Double(new Float(df.format(rentaCliente.getRentaFija() / FACTOR_MILES))
                    .doubleValue());
                }
            }
            String codMov = TablaValores.getValor("CalculoRenta.parametros", "RentaSinAntecedentes", "valor");
            if (rentaCliente.getCodMov() != null || !rentaCliente.getCodMov().equals("")) {
                codMov = rentaCliente.getCodMov();
            }

            if (getLogger().isEnabledFor(Level.DEBUG)){
                getLogger().debug("[actualizaRentasCliente]["+rutcliente
                        +"] persona: " + persona);
            }
            if (getLogger().isEnabledFor(Level.DEBUG)){
                getLogger().debug("[actualizaRentasCliente]["+rutcliente
                        +"] Parametros a ingresar de la renta del cliente en SGC");
                getLogger().debug("[actualizaRentasCliente]["+rutcliente+"] rut: [" + persona.rut + "]");
                getLogger().debug("[actualizaRentasCliente]["+rutcliente+"] dv: [" + persona.digitoVerificador
                        + "]");
                getLogger().debug("[actualizaRentasCliente]["+rutcliente+"] rutConyuge: [" + conyuge.rut + "]");
                getLogger().debug("[actualizaRentasCliente]["+rutcliente+"] dvConyuge: [" + conyuge.dv + "]");
                getLogger().debug("[actualizaRentasCliente]["+rutcliente+"] rentaVariable: [" + rv + "]");
                getLogger().debug("[actualizaRentasCliente]["+rutcliente+"] periodoRentaVar: [" + periodoLiqVar
                        + "]");
                getLogger().debug("[actualizaRentasCliente]["+rutcliente+"] rentaLiquida: [" + rl + "]");
                getLogger().debug("[actualizaRentasCliente]["+rutcliente+"] periodoRentaLiq: [" + periodoLiqVar
                        + "]");
                getLogger().debug("[actualizaRentasCliente]["+rutcliente+"] codMov: [" + codMov + "]");
                getLogger()
                .debug("[actualizaRentasCliente]["+rutcliente+"] Llamando a método: "
                        +"ServiciosClienteBean#actualizaClientePersonaRenta");
            }

            if (getLogger().isEnabledFor(Level.DEBUG)){
            	getLogger().debug("[actualizaRentasCliente]["+rutcliente+"]"
                              +" rentaCliente.getOrigenCalculo() ["+rentaCliente.getOrigenCalculo()+"]"
                              +" rentaCliente.getPeriodoDai() ["+rentaCliente.getPeriodoDai()+"]");
            }

            if("DAI".equalsIgnoreCase(rentaCliente.getOrigenCalculo())){

            	Date fechaDai = null;

            	if (rentaCliente.getPeriodoDai() != null){
            		SimpleDateFormat sd = new SimpleDateFormat("ddMMyyyy", new Locale("es", "CL"));

                	fechaDai =
                         FechasUtil.convierteStringADate(DIA_MES_DAI.concat(rentaCliente.getPeriodoDai()), sd);

                	if (getLogger().isEnabledFor(Level.DEBUG)){
                    	getLogger().debug("[actualizaRentasCliente]["+rutcliente+"] fechaDai["+fechaDai+"]");
            	}
            	}
            	else{
            		fechaDai = new Date();
            		if (getLogger().isEnabledFor(Level.DEBUG)){
                    	getLogger().debug("[actualizaRentasCliente]["+rutcliente+"] fechaDai["+fechaDai+"]");
            	}
            	}

                periodoLiqVar = fechaDai;           	
            }

            if ((rentaCliente.getUsuarioActualiza() == null) || (rentaCliente.getUsuarioActualiza().trim().equals(""))) {
                srvCliente
                    .actualizaClientePersonaRenta(new Long(persona.rut), "" + persona.digitoVerificador,
                        (conyuge.rut != null ? conyuge.rut : ""), strdv + "", rv, periodoLiqVar, rl, periodoLiqVar,
                        codMov);
            }
            else {
                srvCliente
                .actualizaClientePersonaRenta(new Long(persona.rut), "" + persona.digitoVerificador,
                    (conyuge.rut != null ? conyuge.rut : ""), strdv + "", rv, periodoLiqVar, rl, periodoLiqVar,
                    codMov, rentaCliente.getUsuarioActualiza());
            }
            if (getLogger().isEnabledFor(Level.DEBUG)){
                getLogger().debug("[actualizaRentasCliente]["+rutcliente+"] Método ejecutado correctamente");
            }
        }
        catch (TuxedoException e) {
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[actualizaRentasCliente]["+rutcliente+"][BCI_FINEX][TuxedoException] error: "
                        + e.getMessage(), e);
            }
            throw e;
        }
        catch (ClientesException e) {
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[actualizaRentasCliente]["+rutcliente+"][BCI_FINEX][ClientesException] error: "
                        + e.getMessage(), e);
            }
            throw e;
        }
        catch (GeneralException e) {
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[actualizaRentasCliente]["+rutcliente+"][BCI_FINEX][GeneralException] error: "
                        + e.getMessage(), e);
            }
            throw e;
        }
        catch (Exception e) {
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[actualizaRentasCliente]["+rutcliente+"][BCI_FINEX][Exception] error: "
                        + e.getMessage(), e);
            }
            throw new GeneralException("0274");
        }
        if (getLogger().isEnabledFor(Level.INFO)) {
                getLogger().info("[actualizaRentasCliente]["+rutcliente+"][BCI_FINOK]");
            }
        return true;
    }

           /**
             * <p>
     * Método que permite obtener la renta de un cliente.
     * </p>
     * Registro de versiones:
     * <UL>
     * <li>1.0 07/04/2008, Vasco Ariza M. (SEnTRA): versión inicial.</li>
     * <li> 1.1 22/04/2009, Pedro Carmona Escobar (SEnTRA): Se reemplaza la lógica de obtención de la
     * instancia del servicios Cliente por la llamada método {@link #obtenerInstanciaSeriviciosCliente()}.</li>
     * </UL>
     * 
     * @param rut del cliente a consultar.
     * @param dv del cliente a consultar.
     * @return renta del cliente.
     * @throws Exception
     * @since 4.3
     */
    public double obtenerRentaClientePersona(long rut, char dv) throws Exception {
        if (log.isDebugEnabled()) { log.debug("[obtenerRentaClientePersona]: ["+ rut + "-" + dv+"]"); }
        try {
            ServiciosCliente serviciosClientes = obtenerInstanciaSeriviciosCliente();
            if (log.isDebugEnabled()) {
                log.debug("[obtenerRentaClientePersona]Instancia servCli: [" + servCli + "]");
            }
            ClientePersona cl = serviciosClientes.getClientePersona(rut,dv);
            if (log.isDebugEnabled()) {
                log.debug("[obtenerRentaClientePersona]:rentaFija: [" + cl.rentaFija + "]");
            }
            return cl.rentaFija;
        } catch(Exception ex) {
            if (log.isEnabledFor(Level.ERROR)) {
                log.error("[obtenerRentaClientePersona]:Error [" + ex.toString() + "]");
            }
            throw ex;
        }
    }

    /**
     * <p>Método que permite obtener el valor de la UF.</p>
     *
     * Registro de versiones:<UL>
     *
     * <li>1.0 07/04/2008, Vasco Ariza M. (SEnTRA): versión inicial.</li>
     *
     * </UL>
     * @return valor de la UF.
     * @throws Exception
     * @since 4.3
     */
    public double obtenerValorUF() throws Exception {
        log.debug("CotizacionClienteBean.obtenerValorUF");
        try {
            if (log.isDebugEnabled()) {
                log.debug("[obtenerValorUF]Instancia servEconomia: [" + servEconomia + "]");
            }
            InfoFinanciera info = servEconomia.pfiInfoFin(new Date());
            if (log.isDebugEnabled()) {
                log.debug("CotizacionClienteBean.valorUF: [" + info.valorUF + "]");
            }
            return info.valorUF;
        } catch(Exception ex) {
            if (log.isEnabledFor(Level.ERROR)) {
                log.error("[ingresoCotizacionCliente]:Error [" + ex.toString() + "]");
            }
            throw ex;
        }
    }

    /**
     * <p>Método que permite consultar la cotización de un cliente.</p>
     *
     * Registro de versiones:<UL>
     *
     * <li>1.0 07/04/2008, Vasco Ariza M. (SEnTRA): versión inicial.</li>
     *
     * </UL>
     * @param entrada para obtener la cotización.
     * @return cotización del cliente.
     * @throws Exception
     * @since 4.3
     */
    public String consultaCotizacionCliente(String xmlEntrada) throws Exception {
        if (log.isDebugEnabled()) { log.debug("[consultaCotizacionCliente]: xmlEntrada :["+ xmlEntrada +"]"); }
        String retorno = "";
        try {
            String protocolo = TablaValores.getValor(ARCHIVO_PARAMETROS, "CONEXION_PREVIRED", "protocolo");
            String servidor = TablaValores.getValor(ARCHIVO_PARAMETROS, "CONEXION_PREVIRED", "servidor");
            String url = TablaValores.getValor(ARCHIVO_PARAMETROS, "CONEXION_PREVIRED", "url");
            String tiempoEspera = TablaValores.getValor(ARCHIVO_PARAMETROS, "CONEXION_PREVIRED", "tiempoEspera");
            String wsdl = protocolo + "://" + servidor + url;
            if (log.isDebugEnabled()) { log.debug("[consultaCotizacionCliente]: wsdl :["+ wsdl +"]"); }
            CWSMensajeriaXMLService service = new CWSMensajeriaXMLService_Impl(wsdl);
            CWSMensajeriaXML port = service.getCWSMensajeriaXML();
            CWSMensajeriaXML_Stub stub = (CWSMensajeriaXML_Stub)port;
            stub._setProperty("weblogic.webservice.rpc.timeoutsecs", new Integer(tiempoEspera));
            retorno = stub.ejecuta(xmlEntrada);
        } catch (UnsupportedOperationException e) {
            if (log.isEnabledFor(Level.ERROR)) {
                log.error("[consultaCotizacionCliente]:UnsupportedOperationException [" + e.toString() + "]");
            }
            throw new Exception("La operacion solicitada no puede ser realizada");
        } catch (RemoteException e) {
            if (log.isEnabledFor(Level.ERROR)) {
                log.error("[consultaCotizacionCliente]:RemoteException [" + e.toString() + "]");
            }
            throw new Exception("Problemas en la conexion a Previred");
        } catch (IOException e) {
            if (log.isEnabledFor(Level.ERROR)) {
                log.error("[consultaCotizacionCliente]:IOException [" + e.toString() + "]");
            }
            throw new Exception("Problemas de IO");
        }catch (Exception e){
            if (log.isEnabledFor(Level.ERROR)) {
                log.error("[consultaCotizacionCliente]:Exception[" + e.toString() + "]");
            }
            throw new Exception("Problemas al consultar Cotizacion");           
        }
        if (log.isDebugEnabled()) { log.debug("retorno: ["+ retorno +"]"); }
        return retorno;
    }

    /**
     * <p>Método que permite ingresar la cotización de un cliente.</p>
     *
     * Registro de versiones:<UL>
     *
     * <li>1.0 07/04/2008, Vasco Ariza M. (SEnTRA): versión inicial.</li>
     *
     * </UL>
     * @param rut del cliente a ingresar.
     * @param dv del cliente a ingresar.
     * @return respuesta a la operación de ingreso.
     * @throws RemoteException
     * @since 4.3
     */
    public int ingresoCotizacionCliente(CotizacionClienteVO cotizacionCliente) throws Exception {
        int resultado = 0;
        log.debug("CotizacionCliente.ingresoCotizacionCliente: []");
        try{
            CalculoRentaDAO calculoRentaDAO = new CalculoRentaDAO();
            if (log.isDebugEnabled()) {
                log.debug("[ingresaErrorCotizacion]: Luego de instanciar el DAO[" + calculoRentaDAO + "]");
            }
            log.debug("[ingresoCotizacionCliente]: Luego de obtener cotizacion");
            if(cotizacionCliente != null){
                log.debug("[ingresoCotizacionCliente]: Se ingresa encabezado");
                calculoRentaDAO.ingresoCotizacionCliente(cotizacionCliente);
            }
            if(cotizacionCliente.getDetalleCotizacion() != null){
                log.debug("[ingresoCotizacionCliente]: Se ingresa el o los detalles");
                for(int posicion = 0; posicion < cotizacionCliente.getDetalleCotizacion().length; posicion++){
                    resultado = calculoRentaDAO.ingresoDetalleCotizacionCliente(cotizacionCliente, posicion);
                }
            }
        } catch (Exception ex){
            if (log.isEnabledFor(Level.ERROR)) {
                log.error("[ingresoCotizacionCliente]:Error [" + ex.toString() + "]");
            }
            throw ex;
        }
        return resultado;
    }





    /**
     * <p>Método que permite ingresar el error en caso de no poder consultar la o las cotizaciones de un cliente.</p>
     *
     * Registro de versiones:<UL>
     *
     * <li>1.0 07/04/2008, Mauricio Retamal C. (SEnTRA): versión inicial.</li>
     *
     * </UL>
     * @param cotizacionClienteVO posee la información general de la cotización.
     * @param errorCotizacionVO posee el detalle del error producido al consultar la cotización.
     * @return respuesta a la operación de ingreso del error.
     * @throws RemoteException
     * @throws GeneralException
     * @since 4.3
     */
    public int ingresaErrorCotizacion(CotizacionClienteVO cotizacionCliente, ErrorCotizacionVO errorCotizacion) throws Exception{
        int resultado = 0;
        log.debug("CotizacionCliente.ingresaErrorCotizacion: En ingresaErrorCotizacion");

        try {
            CalculoRentaDAO calculoRentaDAO = new CalculoRentaDAO();
            if (log.isDebugEnabled()) {
                log.debug("[ingresaErrorCotizacion]: Luego de instanciar el DAO[" + calculoRentaDAO + "]");
            }
            log.debug("[ingresaErrorCotizacion]: Se ingresa error");
            resultado = calculoRentaDAO.ingresaErrorCotizacion(cotizacionCliente, errorCotizacion);
        } catch (Exception ex) {
            if (log.isEnabledFor(Level.ERROR)) {
                log.error("[ingresaErrorCotizacion]:Error [" + ex.toString() + "]");
            }
            throw ex;
        }
        return resultado;
    }


    /**
     * <p>Método que permite ingresar parte de la información de la Cotización, información que representa el encabezado 
     * de la cotizacion .</p>
     *
     * Registro de versiones:<UL>
     *
     * <li>1.0 08/07/2008, Jaime Gaete L. (SEnTRA): versión inicial.</li>
     * <li>1.1 22/04/2009, Pedro Carmona Escobar (SEnTRA): Se cambia mensaje en líneas de log que identifican al método.
     *
     * </UL>
     * @param cotizacionClienteVO posee la información general de la cotización.
     * @return respuesta a la operación de ingreso.
     * @throws Exception
     * @since 4.3
     */           
    public int ingresarEncabezadoCotizacion(CotizacionClienteVO cotizacionCliente) throws Exception{
        int resultado = 0;
        log.debug("CotizacionCliente.ingresarEncabezadoCotizacion");

        try {
            CalculoRentaDAO calculoRentaDAO = new CalculoRentaDAO();
            if (log.isDebugEnabled()) {
                log.debug("[ingresarEncabezadoCotizacion]: Luego de instanciar el DAO[" + 
                        calculoRentaDAO + "]");
            }
            log.debug("[ingresarEncabezadoCotizacion]: Se ingresa encabezado");
            resultado = calculoRentaDAO.ingresarEncabezadoCotizacion(cotizacionCliente);
            log.debug("[ingresarEncabezadoCotizacion]: Se ingresa el encaebezado");
        } catch (Exception ex) {
            if (log.isEnabledFor(Level.ERROR)) {
                log.error("[ingresarEncabezadoCotizacion]:Error [" + ex.toString() + "]");
            }
            throw ex;
        }
        return resultado;
    }

    /**
     * Método que permite obtener la fecha de permanencia para continuar un cálculo pendiente, 
     * esta fecha se obtiene restando una cierta cantidad de días indicados en la tabla de parametros 
     * "CalculoRenta.parametros" atributo "DiasPermanenciaCalculoPendiente" el que inicialmente es
     * de 30 días.
     * 
     * <p>
     * <b>Registro de versiones:</b>
     * <ul>
     * <li> 1.0 12/09/2008, Manuel Durán M. (Imagemaker IT): versión inicial</li>
     * </ul>
     * </p> 
     * 
     * @return fecha de permanencia para continuar un cálculo pendiente
     * @since 4.6
     */    

    private Date obtieneFechaPermanencia() throws GeneralException {
        try {
            String diasNuevoCalculo = TablaValores.getValor(parametrosNombreArchivo, "DiasPermanenciaCalculoPendiente", "valor");

            int dias = Integer.parseInt(diasNuevoCalculo) * -1;
            Calendar fechaTope = Calendar.getInstance();
            fechaTope.add(Calendar.DATE, dias);
            return fechaTope.getTime();

        } 
        catch (Exception e) {
            log.error("[obtieneFechaPermanencia] Problemas al invocar el método obtieneFechaPermanencia");
            if (log.isEnabledFor(Level.ERROR)) {
                log.error("[obtieneFechaPermanencia] [Exception]" + e.getMessage());
            }
            throw new GeneralException("ESPECIAL", "No fue posible obtener la fecha de permanencia.");
        }

    }

    /**
     * <p>Método que indica si un cliente "Tiene" productos Tbanc.</p>
     * <p>
     * <b>Registro de versiones:</b>
     * <ul>
     * <li> 1.0 01/09/2008, Evelin Sáez G. (Imagemaker IT): versión inicial</li>
     *  <li> 1.1 02/10/2009, Eugenio Contreras (Imagemaker IT): Se agregan líneas de log.</li>
     * </ul>
     * </p> 
     * 
     * @param rutCliente rut del cliente sin dígito verificador
     * @return boolean Retorna true si el cliente "Tiene" productos Tbanc 
     * @throws GeneralException Lanza excepción General
     * @since 4.7
     */        
    private boolean tieneProductosTbanc(long rutCliente) throws GeneralException {
        try {
            log.debug("[tieneProductosTbanc] Iniciando");
            if( log.isDebugEnabled() ){
                log.debug("[tieneProductosTbanc] Rut recibido como parámetro: {" + rutCliente + "}");
            }
            RentaTcorpDAO rentaTcorpDao = new RentaTcorpDAO();

            log.debug("[tieneProductosTbanc] Consultando rentaTcorpDao.tieneProductosTbanc(String");
            boolean resp = rentaTcorpDao.tieneProductosTbanc(rutCliente);

            if( log.isDebugEnabled() ){
                log.debug("[tieneProductosTbanc] Tiene productos TBanc? = {" + resp + "}");
            }                
            return resp;
        }
        catch (Exception e) {
            log.error("[tieneProductosTbanc] Problemas al invocar el método consPrefenciaProductoCliente");
            if( log.isEnabledFor(Level.ERROR) ){
                log.error("[tieneProductosTbanc] [Exception]" + e.getMessage());
            }
            throw new GeneralException("ESPECIAL", "No fue posible grabar los datos de renta.");
        }        
    }

    /**
     * Método que permite deshacer los cambios realizados por el método 
     * {@link #ingresaCalculoRentaTbanc}, volviendo a los valores anteriores de Tcorp.
     * <p>
     * <b>Registro de versiones:</b>
     * <ul>
     * <li> 1.0 01/09/2008, Evelin Sáez G. (Imagemaker IT): versión inicial</li>
     * <li> 1.1 25/05/2010, Roberto Rodríguez(Imagemaker IT): Se agregan líneas de log con el fin de sacarlos a 
     * un archivo diferente </li>
     * <li> 1.2 14/07/2010, Roberto Rodríguez(Imagemaker IT): Se agrega más información en los logs </li> 
     * <li> 1.3 19/03/2015 Alejandro Barra (SEnTRA) - Andrés Alvarado (Ing. Soft. BCI): se elimina variable logCalculo y se agrega llamado a método getLogger.
     * </li>
     * </ul>
     * </p> 
     * 
     * @param rutCliente rut del cliente sin dígito verificador
     * @param graboTcorp es true cuando ingresó la renta en sistema Tcorp 
     * @throws RemoteException Lanza excepción de ejb
     * @since 4.7
     */           
    private void actualizaDatosTcorp(String rutCliente, boolean graboTcorp) throws RemoteException {
        if (getLogger().isEnabledFor(Level.INFO)){
            getLogger().info("[actualizaDatosTcorp][" + rutCliente + "][BCI_INI] inicio graboTcorp["
            + graboTcorp + "]");
        }
        //Si graboTcorp es true significa que ingresó los datos y es posible hacer rollback.
        if (graboTcorp) {
            try {               
                if (getLogger().isEnabledFor(Level.DEBUG)){
                    getLogger().debug("[actualizaDatosTcorp] Invocando método rbkCalculoRentaTbanc.");
                }
                trxBean.rbkCalculoRentaTbanc(rutCliente);
                if (getLogger().isEnabledFor(Level.DEBUG)){
                    getLogger().debug("[actualizaDatosTcorp] La actualización de la renta en TCorp fue exitosa.");
                }
            }
            catch (GeneralException e ) {
            	if(getLogger().isEnabledFor(Level.ERROR)){
                    getLogger().error("[actualizaDatosTcorp][GeneralException] error"
                    + " con mensaje: " + e.getMessage(), e);
                }
                }
            }
        if (getLogger().isEnabledFor(Level.INFO)){
            getLogger().info("[actualizaDatosTcorp][BCI_FINOK]");
        }   
    }   


    /**
     * Método genérico que permite consultar a Previred por la factibildiad de actualizar
     * la renta del cliente a través de sus cotizaciones previsionales. 
     * <p>
     * <b>Registro de versiones:</b>
     * <ul>
     * <li> 1.0 22/04/2009, Pedro Carmona E. (SEnTRA): versión inicial</li>
     * </ul>
     * </p> 
     * 
     * @param rut con el rut del cliente.
     * @param dv con el dígito verificador del rut del cliente.
     * @return boolean que indica si procede o no consultar a Previred.
     * @throws Exception 
     * @since 4.8
     */           
    public boolean obtenerConfirmacionConsultaPrevired(long rut, char dv) throws Exception {
        if (log.isDebugEnabled()) {log.debug("[CalculoRentaBean.obtenerConfirmacionConsultaPrevired] parametros: rut ["+rut+"]   dv ["+dv+"]");}
        boolean validarPrevired;
        try{
            ServiciosCliente serviciosCliente = obtenerInstanciaSeriviciosCliente();
            if (log.isDebugEnabled()) {log.debug("[CalculoRentaBean.obtenerConfirmacionConsultaPrevired] Instancia servCli: [" + servCli + "]"); }
            ClientePersona cl = serviciosCliente.getClientePersona(rut,dv);
            if (log.isDebugEnabled()) {log.debug("[CalculoRentaBean.obtenerConfirmacionConsultaPrevired] rentaFija: [" + cl.rentaFija + "] rentaFija: [" + cl.rentaFija + "]");}
            double rentaActual = cl.rentaFija + cl.rentaVariable;
            double valorUF = obtenerValorUF();
            if (log.isDebugEnabled()) {log.debug("[CalculoRentaBean.obtenerConfirmacionConsultaPrevired] valorUF: [" + valorUF + "]");}
            validarPrevired = validarConsultaPrevired(rentaActual, valorUF);
        }catch(Exception ex){
            if( log.isEnabledFor(Level.ERROR) ){
                log.error("[CalculoRentaBean.obtenerConfirmacionConsultaPrevired] Excepcion validacion Previred: " + ex.toString());
            }
            validarPrevired = false;
        }
        if (log.isDebugEnabled()) {
            log.debug("[CalculoRentaBean.obtenerConfirmacionConsultaPrevired] validarPrevired ["+validarPrevired+"]");
        }
        return validarPrevired;
    }  

    /**
     * Método genérico que permite consultar a Previred por la factibildiad de actualizar
     * la renta del cliente a través de sus cotizaciones previsionales. Este método 
     * agrega un parametro de retorno para identificar si ha ocurrido una exepcion interna 
     * <p>
     * <b>Registro de versiones:</b>
     * <ul>
     * <li> 1.0 07/08/2011, Javier Aguirre A. (Imagemaker): versión inicial</li>
     * </ul>
     * </p> 
     * 
     * @param rut con el rut del cliente.
     * @param dv con el dígito verificador del rut del cliente.
     * @return boolean que indica si procede o no consultar a Previred.
     * @throws Exception flags Es un mapa que retorna dos claves, una para validar previred y otra para identificar excepcion
     * @since 4.8
     */           
    public Map obtenerConfirmacionConsultaPreviredFlag(long rut, char dv) throws Exception {
        if (log.isDebugEnabled()) {log.debug("[CalculoRentaBean.obtenerConfirmacionConsultaPrevired] parametros: rut ["+rut+"]   dv ["+dv+"]");}
        Map flags = new HashMap();
        boolean validarPrevired;

        flags.put("Exepcion", Boolean.valueOf("False"));
        try{
            ServiciosCliente serviciosCliente = obtenerInstanciaSeriviciosCliente();
            if (log.isDebugEnabled()) {log.debug("[CalculoRentaBean.obtenerConfirmacionConsultaPrevired] Instancia servCli: [" + servCli + "]"); }
            ClientePersona cl = serviciosCliente.getClientePersona(rut,dv);
            if (log.isDebugEnabled()) {log.debug("[CalculoRentaBean.obtenerConfirmacionConsultaPrevired] rentaFija: [" + cl.rentaFija + "] rentaFija: [" + cl.rentaFija + "]");}
            double rentaActual = cl.rentaFija + cl.rentaVariable;
            double valorUF = obtenerValorUF();
            if (log.isDebugEnabled()) {log.debug("[CalculoRentaBean.obtenerConfirmacionConsultaPrevired] valorUF: [" + valorUF + "]");}
            validarPrevired = validarConsultaPrevired(rentaActual, valorUF);
            flags.put("validarPrevired", Boolean.valueOf(String.valueOf(validarPrevired)));
        }catch(Exception ex){
            if( log.isEnabledFor(Level.ERROR) ){
                log.error("[CalculoRentaBean.obtenerConfirmacionConsultaPrevired] Excepcion validacion Previred: " + ex.toString());
            }
            validarPrevired = false;
            flags.put("validarPrevired", Boolean.valueOf(String.valueOf(validarPrevired)));
            flags.put("Exepcion", Boolean.valueOf("True"));
        }
        if (log.isDebugEnabled()) {
            log.debug("[CalculoRentaBean.obtenerConfirmacionConsultaPrevired] validarPrevired ["+validarPrevired+"]");
        }
        return flags;
    }  


    /**
      * Método genérico que permite actualizar la renta de un cliente a partir de sus cotizaciones 
      * obtenidas de Previred.
      * 
      * <p>Registro de versiones:<ul>
     * <li> 1.0 22/04/2009, Pedro Carmona E. (SEnTRA): versión inicial</li>
      * <li> 1.1 02/07/2010, Paulina Vera M.  (SEnTRA): Se añade parametro 'codServicio' que indica el
      *         servicio por el cual se está
     * <li>1.2 30/04/2013, Samuel Merino A. (ADA Ltda.): Se modifica método agregando llamada al evaluador de 
     *                                                   rentas el cual se encarga de entregar un valor calculado
     *                                                   para posteriormente realizar la actualización en caso
     *                                                    de ser necesaria</li>
      * <li>1.3 30/07/2014 Eduardo Villagrán Morales (Imagemaker): se mejora y normaliza log
     * <li>1.4 22/09/2014, Manuel Escárate (BEE): Se sobrecarga método y se lleva la lógica.
     * </ul>
      * 
     * 
     * @param canal con el canal por el cual se hace la consulta.
     * @param rut con el rut del cliente.
     * @param dv con el dígito verificador del rut del cliente.
     * @param multiEnvironment con datos para obtener el estado de la renta.
     * @param codServicio codigo servicio.
     * @return boolean indicando si se realizó o no la actualización de la renta.
     * @throws Exception en caso de un error.
     * @since 4.8
     */  
      public boolean actualizarRentaConCotizaciones(String canal, long rut, char dv, MultiEnvironment
              multiEnvironment, String codServicio) throws Exception {
         return actualizarRentaConCotizaciones(canal,rut,dv,multiEnvironment,codServicio,null);
      }   
      
     /**
      * Método genérico que permite actualizar la renta de un cliente a partir de 
      * sus cotizaciones obtenidas de Previred.
      * <p>
      * <b>Registro de versiones:</b>
      * <ul>
      * <li> 1.0 22/09/2014, Manuel Escárate (BEE): versión inicial</li>
      * </ul>
      * </p> 
      * 
      * @param canal con el canal por el cual se hace la consulta.
      * @param rut con el rut del cliente.
      * @param dv con el dígito verificador del rut del cliente.
      * @param multiEnvironment con datos para obtener el estado de la renta.
      * @param codServicio codigo servicio.
      * @param aplicacion aplicacion.
      * @return boolean indicando si se realizó o no la actualización de la renta.
      * @throws Exception en caso de un error.
      * @since 1.0
      */  
     public boolean actualizarRentaConCotizaciones(String canal, long rut, char dv, 
             MultiEnvironment multiEnvironment, String codServicio, String aplicacion) throws Exception {
         if (getLogger().isInfoEnabled()) {
            getLogger().info("[actualizarRentaConCotizaciones] [" + rut + "] [BCI_INI] canal=<" + canal 
                    + ">, multiEnvironment=<" + multiEnvironment + ">, codServicio=<" + codServicio + ">");
         }
          
        String miPerfil = TablaValores.getValor(TABLA_PARAMETROS_CALCULORENTA,
                "actualizacionRentaMiPerfil","aplicacion");
        String simulador = TablaValores.getValor(TABLA_PARAMETROS_CALCULORENTA,
                "actualizacionRentaSimulador","aplicacion");
        
         boolean estadoDeRenta= estadoRenta(rut, dv,multiEnvironment);
        if (aplicacion != null && (aplicacion.equalsIgnoreCase(miPerfil) || aplicacion.equalsIgnoreCase(simulador))
            ){
            estadoDeRenta = getClienteBean().getEstadoRenta(rut, dv,aplicacion);
        }
        
        if (estadoDeRenta) {
            if (getLogger().isInfoEnabled()) {
                getLogger().info("[actualizarRentaConCotizaciones] [" + rut 
                        + "] [BCI_FINOK] Renta ya actualizada. No requiere el proceso. retorna true.");
            }
            return true;
        }
        CotizacionClienteVO cotizacionCliente = null;
        DetalleCotizacionClienteVO[] detCotizCliente = null;
        int codigoRegistro = -1;
        String xmlSalida = "";
        try{
            try {
                cotizacionCliente = obtenerCotizaciones(canal, rut, dv);
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("[actualizarRentaConCotizaciones] [" + rut + "] cotizacionCliente=<"
                            + cotizacionCliente + ">");
                }
            } 
            catch (Exception e){
     		   if (getLogger().isEnabledFor(Level.ERROR)) {
     		       getLogger().error("[actualizarRentaConCotizaciones] [" + rut 
     		               + "] [Exception] [BCI_FINEX] retorna false. " + e.getMessage(), e);
                }
                return false;
            }
            if (cotizacionCliente.esObtenidoDesdePrevired()) { 
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("[actualizarRentaConCotizaciones] [" + rut 
                            + "] cotización es de previred");
                }
                cotizacionCliente = cargarDatosEncabezadoCotizacion(canal, rut, dv, codServicio);
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("[actualizarRentaConCotizaciones] [" + rut + "] cotizacionCliente=<"
                            + cotizacionCliente + ">");
                }
                try {
                    codigoRegistro = ingresarEncabezadoCotizacion(cotizacionCliente);
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("[actualizarRentaConCotizaciones] [" + rut + "] codigoRegistro=<"
                                + codigoRegistro + ">");
                    }
                    cotizacionCliente.setIdentificador(codigoRegistro);
                } 
                catch (Exception e){
                    if (getLogger().isEnabledFor(Level.WARN)) {
                        getLogger().warn("[actualizarRentaConCotizaciones] [" + rut 
                                + "] [Exception] [BCI_FINEX] return false. " + e.getMessage(), e);
                    }
                    return false;
                }
                String xmlEntrada = generarEntradaConsultaPrevired(rut, dv, codigoRegistro);
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("[actualizarRentaConCotizaciones] [" + rut + "] xmlEntrada=<"
                            + xmlEntrada + ">");
                }
                try{
                    xmlSalida = consultaCotizacionCliente(xmlEntrada);
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("[actualizarRentaConCotizaciones] [" + rut + "] xmlSalida=<"
                                + xmlSalida + ">");
                    }
                }
                catch(Exception ex){
     			   if (getLogger().isEnabledFor(Level.WARN)) {
     			       getLogger().warn("[actualizarRentaConCotizaciones] [" + rut + "] [Exception] " 
                            + ex.getMessage(), ex);
                    }
                    try{
                        cotizacionCliente = cargarDatosCotizacionError(canal, rut, dv, codigoRegistro, 
                                TablaValores.getValor(ARCHIVO_PARAMETROS, "EST_RENTA_PROBLEMAPREVIRED", "valor"));
                        if (getLogger().isDebugEnabled()) {
                            getLogger().debug("[actualizarRentaConCotizaciones] [" + rut 
                                    + "] cotizacionCliente=<" + cotizacionCliente + ">");
                        }
                        ErrorCotizacionVO errorCotizacion = new ErrorCotizacionVO();
                        String codError = TablaValores.getValor(
                                ARCHIVO_PARAMETROS, "ERROR_PREVIRED", "errorGenerico");
                        errorCotizacion.setCodigoError(codError);
                        String motivoError = ex.getMessage().length() > DET_ERROR_COT 
                                ? ex.getMessage().substring(0,DET_ERROR_COT): ex.getMessage();
                        errorCotizacion.setDetalleError(motivoError);
                        if (getLogger().isDebugEnabled()) {
                            getLogger().debug("[actualizarRentaConCotizaciones] [" + rut + "] codError=<"
                                    + codError + ">, motivoError=<" + motivoError + ">");
                        }
                        int respuesta = ingresaErrorCotizacion(cotizacionCliente, errorCotizacion);
     					if (getLogger().isDebugEnabled()) {
                            getLogger().debug("[actualizarRentaConCotizaciones] [" + rut + "] respuesta=<"
                                    + respuesta + ">");
                        }
                    }
                    catch(Exception e){
                        if (getLogger().isEnabledFor(Level.ERROR)) {
                            getLogger().error("[actualizarRentaConCotizaciones] [" + rut 
                                    + "] [Exception] [BCI_FINEX] retorna false. " + e.getMessage(), e);
                        }
                    }
                    return false;
                } 
                String controlCotizacion = validarErrorCotizacionCliente(xmlSalida);
                String cotizSinError = TablaValores.getValor(ARCHIVO_PARAMETROS, "COTIZACIONSINERROR", "valor");
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("[actualizarRentaConCotizaciones] [" + rut + "] controlCotizacion=<"
                            + controlCotizacion + ">, cotizSinError=<" + cotizSinError + ">");
                }
                if (controlCotizacion.equalsIgnoreCase(cotizSinError)){
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("[actualizarRentaConCotizaciones] [" + rut + "] Cotizacion sin error");
                    }
                    cotizacionCliente = cargarDatosCotizacion(rut, dv, canal, xmlSalida, codigoRegistro);
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("[actualizarRentaConCotizaciones] [" + rut + "] cotizacionCliente=<"
                                + (cotizacionCliente!=null?cotizacionCliente.toString():"null" + ">"));
                    }
                    
                    detCotizCliente = cargarDatosDetalleCotizacion(xmlSalida);
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("[actualizarRentaConCotizaciones] [" + rut + "] detCotizCliente=<"
                                + (detCotizCliente != null? detCotizCliente.toString(): "null" + ">"));
                    }
                    if (cotizacionCliente != null){
                    cotizacionCliente.setDetalleCotizacion(detCotizCliente);
                    }
                    CuentasDelegate cuentasDelegate = new CuentasDelegate();
                    ListaCuentas listaCuentas = cuentasDelegate.cuentasCorrientesPorRut(rut, dv);
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("[actualizarRentaConCotizaciones] [" + rut + "] listaCuentas=<"
                                + listaCuentas + ">");
                    }
                    boolean isClienteVigente = false;

                    if (listaCuentas != null && listaCuentas.cuentas !=null && listaCuentas.cuentas.length > 0) {
                        isClienteVigente = true;
                    } 
                    else {
                        isClienteVigente = false;
                    }

                    if (log.isDebugEnabled()) {
                        log.debug("[actualizarRentaConCotizaciones] [" + rut + "] isClienteVigente=<" 
                                + isClienteVigente + ">");
                    }
                    ServiciosCliente servicioCliente = obtenerInstanciaSeriviciosCliente();
                    ClientePersona clientePersona = servicioCliente.getClientePersona(rut, dv);
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("[actualizarRentaConCotizaciones] [" + rut + "] clientePersona=<"
                                + clientePersona + ">");
                    }
                    RentaSolicitanteTO rentaSolicitanteTO = new RentaSolicitanteTO();
                    rentaSolicitanteTO.setCotizacionClienteVO(cotizacionCliente);
                    rentaSolicitanteTO.setRentaFija(clientePersona.getRentaFija());
                    rentaSolicitanteTO.setRentaVariable(clientePersona.getRentaVariable());
                    ReglaRentaTO reglaRenta = evaluarReglas(rentaSolicitanteTO, isClienteVigente);
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("[actualizarRentaConCotizaciones] [" + rut + "] reglaRenta=<"
                                + (reglaRenta != null? reglaRenta.toString():"null"+ ">"));
                    }
                    
                    boolean  montoValido = false;
                    if (reglaRenta != null){
                    if(aplicacion.equalsIgnoreCase(simulador) && canal.equals(CANAL_NOVA)){
                        return true;
                    }
                    
                    if (aplicacion.equalsIgnoreCase(miPerfil) && canal.equals(CANAL_NOVA)){
                        montoValido = validaRentaNova(reglaRenta);
                        if (!montoValido){
                            return true;
                        }
                    }
                    
                    if (!reglaRenta.isCumpleRegla()) {
                        if (getLogger().isDebugEnabled()) {
                            getLogger().debug("[actualizarRentaConCotizaciones] [" + rut 
                                    + "] rentaRegla.isCumpleRegla()=<false>");
                        }
                        cotizacionCliente.setEstadoRenta(TablaValores.getValor(ARCHIVO_PARAMETROS,
                                "EST_RENTA_NOCALCULADA", "valor"));
                        cotizacionCliente.setMontoRenta(0);

                    }
                    if (reglaRenta.isCumpleRegla()) {
                        if (getLogger().isDebugEnabled()) {
                            getLogger().debug("[actualizarRentaConCotizaciones] [" + rut 
                                    + "] rentaRegla.isCumpleRegla()=<true>");
                        }
                        cotizacionCliente = actualizarRentaCalculada(rut, dv, rentaSolicitanteTO, reglaRenta,
                                isClienteVigente,aplicacion);
                        if (getLogger().isDebugEnabled()) {
                            getLogger().debug("[actualizarRentaConCotizaciones] [" + rut 
                                    + "]  cotizacionCliente=<" + cotizacionCliente + ">");
                        }
                    }


                    int ingresoCotizacion = ingresoCotizacionCliente(cotizacionCliente);
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("[actualizarRentaConCotizaciones] [" + rut + "] ingresoCotizacion=<"
                                + ingresoCotizacion + ">");
                    }
                
                    if (aplicacion != null && (aplicacion.equalsIgnoreCase(miPerfil) 
                            || aplicacion.equalsIgnoreCase(simulador))){
                        int resultado = this.ingresarRegistroRTA(cotizacionCliente);
                        if (log.isDebugEnabled()) {
                            log.debug("[CalculoRentaBean][ingresarRegistroRTA] ingresarRegistroRTA:"
                                    + resultado);
                        }
                    }
                  }  
                } 
                else {
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("[actualizarRentaConCotizaciones] [" + rut + "] cotización con error");
                    }
                    String estadoCotizacionNoActualizada = TablaValores.getValor(ARCHIVO_PARAMETROS,
                            "EST_RENTA_NOCALCULADA", "valor");
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("[actualizarRentaConCotizaciones] [" + rut 
                                    + "] estadoCotizacionNoActualizada=<" + estadoCotizacionNoActualizada + ">");
                    }
                    cotizacionCliente = cargarDatosCotizacionError(canal, rut, dv, codigoRegistro,
                            estadoCotizacionNoActualizada);
                    ErrorCotizacionVO errorCotizacion = new ErrorCotizacionVO();
                    errorCotizacion.setCodigoError(controlCotizacion);
                    errorCotizacion.setDetalleError("");

                    int respuesta = ingresaErrorCotizacion(cotizacionCliente, errorCotizacion);
                    if (getLogger().isInfoEnabled()) {
                        getLogger().info("[actualizarRentaConCotizaciones] [" + rut
                                    + "] [BCI_FINOK] respuesta=<" + respuesta + ">. Retorno false.");
                    }
                    return false;
                }                                                         
            }                                                         
            else {
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("[actualizarRentaConCotizaciones] [" + rut 
                            + "] cotización no obtenida de previred");
                }
                CuentasDelegate cuentasDelegate = new CuentasDelegate();
                ListaCuentas listaCuentas = cuentasDelegate.cuentasCorrientesPorRut(rut, dv);
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("[actualizarRentaConCotizaciones] [" + rut + "] listaCuentas=<"
                            + listaCuentas + ">");
                }

                boolean isClienteVigente = false;
                if (listaCuentas != null && listaCuentas.cuentas !=null && listaCuentas.cuentas.length > 0) {
                    isClienteVigente = true;
                } 
                else {
                    isClienteVigente = false;
                    }
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("[actualizarRentaConCotizaciones] [" + rut 
                                    + "] isClienteVigente=<" + isClienteVigente + ">");
                }
                ServiciosCliente servicioCliente = obtenerInstanciaSeriviciosCliente();
                ClientePersona clientePersona = servicioCliente.getClientePersona(rut, dv);
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("[actualizarRentaConCotizaciones] [" + rut 
                                    + "] clientePersona=<" + clientePersona + ">");
                }
                RentaSolicitanteTO rentaSolicitanteTO = new RentaSolicitanteTO();
                rentaSolicitanteTO.setCotizacionClienteVO(cotizacionCliente);
                rentaSolicitanteTO.setRentaFija(clientePersona.getRentaFija());
                rentaSolicitanteTO.setRentaVariable(clientePersona.getRentaVariable());
                ReglaRentaTO reglaRenta = evaluarReglas(rentaSolicitanteTO, isClienteVigente);
                if (reglaRenta != null){
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("[actualizarRentaConCotizaciones] [" + rut + "] reglaRenta=<"
                            + reglaRenta + ">");
                }
                if (!reglaRenta.isCumpleRegla()) {
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("[actualizarRentaConCotizaciones] [" + rut + "] validaciones OK");
                    }
                    cotizacionCliente.setEstadoRenta(TablaValores.getValor(ARCHIVO_PARAMETROS,
                            "EST_RENTA_NOCALCULADA", "valor"));
                    cotizacionCliente.setMontoRenta(0);
                }
                if (reglaRenta.isCumpleRegla()) {
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("[actualizarRentaConCotizaciones] [" + rut + "] validaciones OK");
                    }
                    cotizacionCliente = actualizarRentaCalculada(rut, dv, rentaSolicitanteTO, reglaRenta,
                            isClienteVigente,aplicacion);
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("[actualizarRentaConCotizaciones] [" + rut 
                                    + "] cotizacionCliente=<" + cotizacionCliente + ">");
                }
                }

                int ingresoCotizacion = ingresoCotizacionCliente(cotizacionCliente);
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("[actualizarRentaConCotizaciones] [" + rut + "] ingresoCotizacion=<"
                            + ingresoCotizacion + ">");
                }
                if (cotizacionCliente.getEstadoRenta().equals(
                        TablaValores.getValor(ARCHIVO_PARAMETROS, "EST_RENTA_ACTUALIZADA", "valor"))) {

                    int resultado = actualizaEstadoEncabezado(cotizacionCliente.getIdentificador(),
                            cotizacionCliente.getEstadoRenta());
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("[actualizarRentaConCotizaciones] [" + rut + "] resultado=<"
                                + resultado + ">");
                    }
                } 
                else {
                    if (getLogger().isInfoEnabled()) {
                        getLogger().info("[actualizarRentaConCotizaciones] [" + rut 
                                    + "] [BCI_FINOK] renta no actualizada. retorna false.");
                    }
                    return false;
                }
            }
        } 
        } 
        catch (Exception ex) {
     	   if (getLogger().isEnabledFor(Level.WARN)) {
     	       getLogger().warn("[actualizarRentaConCotizaciones] [" + rut + "] [Exception] "
     	               + ex.getMessage(), ex);
            }
            ErrorCotizacionVO errorCotizacion = new ErrorCotizacionVO();
            try{
                String codError = TablaValores.getValor(ARCHIVO_PARAMETROS, "ERROR_PREVIRED", "errorGenerico");
                errorCotizacion.setCodigoError(codError);
                errorCotizacion.setDetalleError(ex.getMessage().length() > DET_ERROR_COT 
                        ? ex.getMessage().substring(0,DET_ERROR_COT): ex.getMessage());
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("[actualizarRentaConCotizaciones] [" + rut + "] codError=<" 
                            + codError + ">");
                }
                if(codigoRegistro != -1){
                    cotizacionCliente = cargarDatosCotizacionError(
                            canal, rut, dv, codigoRegistro, TablaValores.getValor(
                                    ARCHIVO_PARAMETROS, "EST_RENTA_PROBLEMAPREVIRED", "valor"));
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("[actualizarRentaConCotizaciones] [" + rut + "] cotizacionCliente=<"
                                + cotizacionCliente + ">");
                    }
                    int respuesta = ingresaErrorCotizacion(cotizacionCliente, errorCotizacion);
     				if (getLogger().isDebugEnabled()) {
                        getLogger().debug("[actualizarRentaConCotizaciones] [" + rut + "] respuesta=<"
                                + respuesta  + ">");
                    }                    
                    }                    
                else{
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("[actualizarRentaConCotizaciones] [" + rut 
                                + "] No se pudo ingresar Registro, problema al obtener el identificador.");
                }                       
                }
            }
            catch(Exception e){
     		   if (getLogger().isEnabledFor(Level.WARN)) {
     		       getLogger().warn("[actualizarRentaConCotizaciones] [" + rut + "] [Exception] "
                        + e.getMessage(), e);
     		   }
     		}
     		if (getLogger().isInfoEnabled()) {
                getLogger().info("[actualizarRentaConCotizaciones] [" + rut 
                        + "] [Exception] [BCI_FINEX] retorna false. " + ex.getMessage(), ex);
            }
            return false;
        }
        if (getLogger().isInfoEnabled()) {
            getLogger().info("[actualizarRentaConCotizaciones] [" + rut + "] [BCI_FINOK] retorna true");
        }
        return true;
    }






    public void setSessionContext(javax.ejb.SessionContext ejbSessionContext) {
        this.ejbSessionContext = ejbSessionContext;
    }

    public void unsetSessionContext() {
        this.ejbSessionContext = null;
    }

    public void ejbRemove() throws javax.ejb.EJBException {
        // TBD:  Do any processing here when instance is being removed
    }

    public void ejbActivate() {
        // TBD:  Restore any saved resources
    }

    public void ejbPassivate() {
        // TBD:  Save any resources here before bean is passivated
    }


    /**
     * <p>Método que permite obtener un String a partir de un objeto de tipo Document</p>
     *
     * Registro de Versiones:<ul>
     *
     * <LI>1.0  (22/04/2008 Pedro Carmona Escobar (SEnTRA)): versión inicial.</li>
     *
     * </UL>
     *
     * @param doc objeto Document que se desea transformar.
     * @return String resultante de la conversión.
     * @since 4.8
     */
    private String obtieneStringDesdeDocument(Document doc)
    {
        try
        {
            DOMSource domSource = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");
            transformer.transform(domSource, result);
            return writer.toString();
        }
        catch(TransformerException ex)
        {
            if( log.isEnabledFor(Level.ERROR) ){
                log.error("[CalculoRentaBean.obtieneStringDesdeDocument] Excepcion : " + ex);
            }
            return null;
        }
    }



    /**
     * <p>Método que permite obtener una instancia del ejb ServiciosCliente.
     *
     * Registro de Versiones:<ul>
     *
     * <LI>1.0  (22/04/2009 Pedro Carmona Escobar (SEnTRA)): versión inicial.</li>
     *
     * </UL>
     * @throws Exception
     * @return con la instancia del ejb.
     * @since 4.8
     */

    private ServiciosCliente obtenerInstanciaSeriviciosCliente() throws Exception {
        if (servCli == null) {
            try {
                EnhancedServiceLocator locator = EnhancedServiceLocator.getInstance();
                ServiciosClienteHome servCliHome = (ServiciosClienteHome) locator.getHome("wcorp.serv.clientes.ServiciosCliente", ServiciosClienteHome.class);
                servCli = servCliHome.create();
            } catch (Exception e) {
                if( log.isEnabledFor(Level.ERROR) ){
                    log.error("[CalculoRentaBean.obtenerInstanciaSeriviciosCliente]:: Excepcion : " + e);
                }
                throw new Exception("Error al crear instancias del ejb ServiciosCliente");
            }
        }
        return servCli;
    }


    /**
     * Método que permite agrupar los datos de los detalles de la o las cotizaciones.
     *
     * <p>Registro de Versiones:<ul>
     * <li>1.0  (22/04/2009 Pedro Carmona Escobar (SEnTRA)): versión inicial.</li>
     * <li>1.1 03/09/2014 Eduardo Villagrán Morales (Imagemaker): Se modifica para formateo de fechas.
     *      Se normaliza log.</li>
     * </ul>
     *
     * @throws Exception al haber error.
     * @param xmlSalida representa el xml a validar.
     * @return objeto con la información de la cotización realizada.
     * @since 4.8
     */
    private DetalleCotizacionClienteVO[] cargarDatosDetalleCotizacion(String xmlSalida) throws Exception{
        if (getLogger().isInfoEnabled()) {
            getLogger().info("[cargarDatosDetalleCotizacion] [BCI_INI] xmlSalida=<" + xmlSalida + ">");
        }
        DetalleCotizacionClienteVO[] remuneracionCompleta = null;
        String remuneracionImp = "";
        String monto = "";
        long rutEmp=0;
        char dvEmp=' ';
        try{
            String detalleRespuesta = TablaValores.getValor(ARCHIVO_PARAMETROS, "TAG_XML_PREVIRED", "respuesta");
            String strMes = TablaValores.getValor(ARCHIVO_PARAMETROS, "VALOR_XML_PREVIRED", "mes");
            String strAfp = TablaValores.getValor(ARCHIVO_PARAMETROS, "VALOR_XML_PREVIRED", "afp");
            String tipomovimiento = TablaValores.getValor(
                    ARCHIVO_PARAMETROS, "VALOR_XML_PREVIRED", "tipomovimiento");
            String strFecPago = TablaValores.getValor(ARCHIVO_PARAMETROS, "VALOR_XML_PREVIRED", "fechapago");
            String strRutEmp = TablaValores.getValor(ARCHIVO_PARAMETROS, "VALOR_XML_PREVIRED", "rutempleador");
            String strRemunImp = TablaValores.getValor(
                    ARCHIVO_PARAMETROS, "VALOR_XML_PREVIRED", "remuneracionimponible");
            String strMonto = TablaValores.getValor(ARCHIVO_PARAMETROS, "VALOR_XML_PREVIRED", "monto");
            Document doc = PreviredUtil.convierteStringADOM(xmlSalida);
            doc.getDocumentElement().normalize ();
            NodeList listaDeElementos = doc.getElementsByTagName(detalleRespuesta);
            int largoElementos = listaDeElementos.getLength();
            remuneracionCompleta = new DetalleCotizacionClienteVO[largoElementos];
            for(int i=0; i<largoElementos ; i++){
                remuneracionCompleta[i] = new DetalleCotizacionClienteVO();
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("[cargarDatosDetalleCotizacion] remuneracionCompleta[" + i + "]=<"
                            + remuneracionCompleta[i] + ">");
                }
                Node primerNodo = listaDeElementos.item(i);
                if(primerNodo.getNodeType() == Node.ELEMENT_NODE){
                    Element primerNodoElement = (Element)primerNodo;
                    String periodo = primerNodoElement.getAttribute(strMes);
                    String afp = primerNodoElement.getAttribute(strAfp);
                    String tipoMovimiento = primerNodoElement.getAttribute(tipomovimiento);
                    String fechaPago = primerNodoElement.getAttribute(strFecPago);
                    String formatoFecha = TablaValores.getValor(ARCHIVO_PARAMETROS, "FMTFECHAAFP", 
                            StringUtil.reemplazaTodo(afp, " ", "").toUpperCase());
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("[cargarDatosDetalleCotizacion] formatoFecha=<" + formatoFecha + ">");
                    }
                    SimpleDateFormat sd = new SimpleDateFormat(
                            formatoFecha==null?FORMATO_FECHA_DD_MM_YYYY: formatoFecha, new Locale("es", "CL"));
                    Date fechaPagoCotiz = FechasUtil.convierteStringADate(fechaPago, sd);

                    String rutEmpleador = primerNodoElement.getAttribute(strRutEmp);
                    if(rutEmpleador!=null && !rutEmpleador.equalsIgnoreCase("")){
                        String[] strRut = StringUtil.divide(rutEmpleador, "-");
                        rutEmp = Long.parseLong(StringUtil.reemplazaCaracteres(strRut[0], ".", ""));
                        dvEmp = strRut[1].charAt(0);
                    }
                    if(primerNodoElement.getAttribute(strRemunImp)!=null){
                        remuneracionImp = primerNodoElement.getAttribute(strRemunImp);
                    }
                    if(primerNodoElement.getAttribute(strMonto)!=null){
                        monto = primerNodoElement.getAttribute(strMonto);
                    }
                    double remuneracionImponible = Double.parseDouble(remuneracionImp);
                    double montoImponible = Double.parseDouble(monto);
                    remuneracionCompleta[i].setPeriodo(periodo);
                    remuneracionCompleta[i].setRemuneracionImponible(remuneracionImponible);
                    remuneracionCompleta[i].setMontoCotizacion(montoImponible);
                    remuneracionCompleta[i].setTipoMovimiento(tipoMovimiento);
                    remuneracionCompleta[i].setAfp(afp);
                    remuneracionCompleta[i].setDvEmpleador(dvEmp);
                    remuneracionCompleta[i].setRutEmpleador(rutEmp);
                    remuneracionCompleta[i].setFechaPago(fechaPagoCotiz);
                }
            }
        }
        catch(Exception ex){
		    if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[cargarDatosDetalleCotizacion] [Exception] [BCI_FINEX] " + ex.getMessage(), ex);
            }
            throw new Exception("Error al ingresar los datos en VO.");
        }
        if (getLogger().isInfoEnabled()) {
            getLogger().info("[cargarDatosDetalleCotizacion] [BCI_FINOK] retorno=<" + remuneracionCompleta + ">");
        }
        return remuneracionCompleta;
    }


    /**
     * <p>Método que permite agrupar los datos de la cotización</p>
     *
     * Registro de Versiones:<ul>
     *
     * <LI>1.0  (22/04/2009 Pedro Carmona Escobar (SEnTRA)): versión inicial.</li>
     *
     * </UL>
     * @throws Exception
     * @param rutCliente rut del cliente.
     * @param dvCliente digito verificador del rut.
     * @param canal con el canal por el cual se hace la consulta.
     * @param xmlSalida representa el xml retornado por previred.
     * @param idRegistro identificador de la solicitud de información a Previred.
     * @return CotizacionClienteVO con la información de la cotización realizada.
     * @since 4.8
     */
    public CotizacionClienteVO cargarDatosCotizacion(long rutCliente, char dvCliente, String canal, String xmlSalida, int identificadoRegistro) throws Exception{
        if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.cargarDatosCotizacion]:: xmlSalida ["+xmlSalida+"]");}
        CotizacionClienteVO cotizacionCliente = null;
        try{
            String folio = "";
            String firma = "";
            String respuestaLegal = TablaValores.getValor(ARCHIVO_PARAMETROS, "TAG_XML_PREVIRED", "respuestaLegal");
            String strFolio = TablaValores.getValor(ARCHIVO_PARAMETROS, "VALOR_XML_PREVIRED", "folio");
            String strFirma = TablaValores.getValor(ARCHIVO_PARAMETROS, "VALOR_XML_PREVIRED", "firma");
            Document doc = PreviredUtil.convierteStringADOM(xmlSalida);
            if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.cargarDatosCotizacion]:: doc ["+doc+"]");}
            doc.getDocumentElement().normalize();
            NodeList listaDeElementos = doc.getElementsByTagName(respuestaLegal);
            int cantidadCotizaciones = listaDeElementos.getLength();
            if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.cargarDatosCotizacion]:: cantidadCotizaciones ["+cantidadCotizaciones+"]");}
            if(cantidadCotizaciones>0){
                cotizacionCliente = new CotizacionClienteVO();
                Node primerNodo = listaDeElementos.item(0);
                if(primerNodo.getNodeType() == Node.ELEMENT_NODE){
                    Element primerNodoElement = (Element)primerNodo;
                    folio=primerNodoElement.getAttribute(strFolio);
                    if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.cargarDatosCotizacion]:: folio ["+folio+"]");}
                    firma=primerNodoElement.getAttribute(strFirma);
                    if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.cargarDatosCotizacion]:: firma ["+firma+"]");}
                }
                Date fechaConsulta = new Date();
                cotizacionCliente.setRutCliente(rutCliente);
                cotizacionCliente.setDvCliente(dvCliente);
                cotizacionCliente.setCanal(canal);
                String servicio = TablaValores.getValor(ARCHIVO_PARAMETROS, "CODIGO_SERVICIO", "avance_multicredito");
                cotizacionCliente.setServicio(servicio);
                cotizacionCliente.setFirma(firma);
                cotizacionCliente.setFolio(folio);
                cotizacionCliente.setFechaConsulta(fechaConsulta);
                cotizacionCliente.setEstadoError("");
                cotizacionCliente.setIdentificador(identificadoRegistro);
                if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.cargarDatosCotizacion]:: cotizacionCliente = " +cotizacionCliente);}
            }
        }catch(Exception ex){
            if( log.isEnabledFor(Level.ERROR) ){
                log.error("[CalculoRentaBean.cargarDatosCotizacion]:: Excepcion : " + ex);
            }
            throw new Exception("Error al ingresar los datos en VO.");
        }
        return cotizacionCliente;
    }



    /**
     * <p>Método que permite validar si se cuenta con la cantidad de cotizaciones necesarias
     * para actualizar en previred..</p>
     *
     * Registro de Versiones:<ul>
     *
     * <LI>1.0  (22/04/2009 Pedro Carmona Escobar (SEnTRA)): versión inicial.</li>
     *
     * </UL>
     * @throws Exception
     * @param xmlSalida representa el xml a transformar.
     * @return representa respuesta a si se puede consultar previred.
     * @since 4.8
     */
    public boolean validarCotizacionCalculoRenta(String xmlSalida) throws Exception {
        boolean validaCotizacion = false;
        Calendar fechaInicio = null;
        Calendar fechaFin = null;
        SimpleDateFormat sdf = new SimpleDateFormat("MMyyyy");
        if(log.isDebugEnabled()){log.debug("[CalculoRentaBean.validarCotizacionCalculoRenta]:: xmlSalida ["+xmlSalida+"]");}
        try{
            String detalleRespuesta = TablaValores.getValor(ARCHIVO_PARAMETROS, "TAG_XML_PREVIRED", "respuesta");
            if(log.isDebugEnabled()){log.debug("[CalculoRentaBean.validarCotizacionCalculoRenta]:: detalleRespuesta ["+detalleRespuesta+"]");}
            String strMes = TablaValores.getValor(ARCHIVO_PARAMETROS, "VALOR_XML_PREVIRED", "mes");
            String fechaPago = TablaValores.getValor(ARCHIVO_PARAMETROS, "VALOR_XML_PREVIRED", "fechapago");
            if(log.isDebugEnabled()){log.debug("[CalculoRentaBean.validarCotizacionCalculoRenta]:: strMes ["+strMes+"]");}
            Document doc = PreviredUtil.convierteStringADOM(xmlSalida);
            doc.getDocumentElement().normalize ();
            NodeList listaDeElementos = doc.getElementsByTagName(detalleRespuesta);
            int y = listaDeElementos.getLength();
            String[] periodo=new String[listaDeElementos.getLength()];
            String[] fechasPagos=new String[listaDeElementos.getLength()];
            for(int s=0; s<y ; s++){
                Node primerNodo = listaDeElementos.item(s);
                if(primerNodo.getNodeType() == Node.ELEMENT_NODE){
                    Element primerNodoElement = (Element)primerNodo;
                    periodo[s]=primerNodoElement.getAttribute(strMes);
                    fechasPagos[s]=primerNodoElement.getAttribute(fechaPago);
                }
            }
            String periodoAnterior="";
            int cont=0;
            ArrayList arregloDeFechas = new ArrayList();
            String fechasMismoPeriodo = "";
            for(int s=0; s<y; s++) {
                if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.validarCotizacionCalculoRenta]:: período ["+periodo[s]+"]");}
                if (!periodoAnterior.equalsIgnoreCase(periodo[s])) {
                    fechaInicio = FechasUtil.convierteStringACalendar(periodo[s], sdf);
                    fechaFin = FechasUtil.convierteStringACalendar(periodoAnterior, sdf);
                    if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.validarCotizacionCalculoRenta]:: fechaInicio ["+fechaInicio.getTime()+"]");}             
                    if(fechaFin!=null){
                        if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.validarCotizacionCalculoRenta]:: fechaFin ["+fechaFin.getTime()+"]");}
                        fechaInicio.add(Calendar.MONTH, 1);
                        if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.validarCotizacionCalculoRenta]:: fechaInicio+1MES ["+fechaInicio.getTime()+"]");}

                        if(!fechaInicio.equals(fechaFin) && cont < CANTIDAD_COTIZACIONES ){
                            if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.validarCotizacionCalculoRenta]:: Se encontró laguna");}
                            cont = 0;
                            validaCotizacion = false;
                            break;                          
                        }else{
                            if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.validarCotizacionCalculoRenta]:: fechasIguales o laguna encontrada pero con 12 cotizaciones consecutivas.");}
                            if (cont<CANTIDAD_COTIZACIONES && arregloDeFechas.contains(fechasPagos[s].substring(3))){
                                if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.validarCotizacionCalculoRenta]:: fechas repetida en periodo anterior ["+fechasPagos[s].substring(3)+"]");}
                                cont = 0;
                                validaCotizacion = false;
                                break;
                            }
                        }
                    }
                    arregloDeFechas.add(fechasPagos[s].substring(3));
                    fechasMismoPeriodo=fechasPagos[s].substring(3);
                    periodoAnterior=periodo[s];
                    cont++;
                    if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.validarCotizacionCalculoRenta]:: cont ["+cont+"]");}
                }else {
                    if (cont<CANTIDAD_COTIZACIONES && arregloDeFechas.contains(fechasPagos[s].substring(3))){
                        if (fechasMismoPeriodo.indexOf(fechasPagos[s].substring(3))==-1){
                            if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.validarCotizacionCalculoRenta]:: fechas repetida en periodo anterior ["+fechasPagos[s].substring(3)+"]");}
                            cont = 0;
                            validaCotizacion = false;
                            break;
                        }
                    }
                    fechasMismoPeriodo = fechasMismoPeriodo + fechasPagos[s].substring(3);
                    arregloDeFechas.add(fechasPagos[s].substring(3));
                }
            }
            if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.validarCotizacionCalculoRenta]:: cantidad de cotizaciones ["+cont+"]");}
            if (cont>=CANTIDAD_COTIZACIONES) {
                validaCotizacion = true;
            } 
        }catch(Exception ex){
            if( log.isEnabledFor(Level.ERROR) ){
                log.error("[CalculoRentaBean.validarCotizacionCalculoRenta]::Excepcion : " + ex);
            } 
            throw new Exception("Error al validar Cotizacion Calculo Renta.");
        }
        return validaCotizacion;
    }





    /**
     * <p>Método que permite validar los estados de error retornados desde previred.</p>
     *
     * Registro de Versiones:<ul>
     *
     * <LI>1.0  (22/04/2009 Pedro Carmona Escobar (SEnTRA)): versión inicial.</li>
     *
     * </UL>
     * @throws Exception
     * @param xmlSalida representa el xml a validar.
     * @return representa el código de error producido.
     * @since 4.8
     */
    private String validarErrorCotizacionCliente(String xmlSalida) throws Exception {
        if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.validarErrorCotizacionCliente] xmlSalida ["+xmlSalida+"]");}
        try{
            String control = TablaValores.getValor(ARCHIVO_PARAMETROS, "TAG_XML_PREVIRED", "tagControl");
            String codControl = TablaValores.getValor(ARCHIVO_PARAMETROS, "TAG_XML_PREVIRED", "codigoControl");
            String codSinError = TablaValores.getValor(ARCHIVO_PARAMETROS, "ERROR_PREVIRED", "sinError");
            String codErrorUsu = TablaValores.getValor(ARCHIVO_PARAMETROS, "ERROR_PREVIRED", "errorUsuario");
            String codErrorClave = TablaValores.getValor(ARCHIVO_PARAMETROS, "ERROR_PREVIRED", "errorClave");
            String codErrorClaveUsu = TablaValores.getValor(ARCHIVO_PARAMETROS, "ERROR_PREVIRED", "errorUsuClave");
            String codSrvCorrecto = TablaValores.getValor(ARCHIVO_PARAMETROS, "ERROR_PREVIRED", "srvCorrecto");

            Document doc = PreviredUtil.convierteStringADOM(xmlSalida);
            if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.validarErrorCotizacionCliente] doc ["+doc+"]");}
            doc.getDocumentElement().normalize();
            NodeList listaDeElementos = doc.getElementsByTagName(control);
            int cantidadCotizaciones = listaDeElementos.getLength();
            if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.validarErrorCotizacionCliente] cantidadCotizaciones ["+cantidadCotizaciones+"]");}

            //CONTROL GENERAL
            Node primerNodo = listaDeElementos.item(0);
            if(primerNodo.getNodeType() == Node.ELEMENT_NODE){
                Element primerNodoElement = (Element)primerNodo;
                String codigoControl=primerNodoElement.getAttribute(codControl);
                if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.validarErrorCotizacionCliente] codigo ["+codigoControl+"]");}
                if(codigoControl.equalsIgnoreCase(codSinError)){
                    return codigoControl;
                }
            }
            //CONTROL Autenticación
            Node segundoNodo = listaDeElementos.item(1);
            if(segundoNodo.getNodeType() == Node.ELEMENT_NODE){
                Element primerNodoElement = (Element)segundoNodo;
                String codigoControl=primerNodoElement.getAttribute(codControl);
                if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.validarErrorCotizacionCliente] codigo ["+codigoControl+"]");}
                if(codigoControl.equalsIgnoreCase(codErrorUsu) || codigoControl.equalsIgnoreCase(codErrorClave) || codigoControl.equalsIgnoreCase(codErrorClaveUsu)){
                    return codigoControl;
                }
            }
            //CONTROL Cotizacion
            Node tercerNodo = listaDeElementos.item(2);
            if(tercerNodo.getNodeType() == Node.ELEMENT_NODE){
                Element primerNodoElement = (Element)tercerNodo;
                String codigoControl=primerNodoElement.getAttribute(codControl);
                if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.validarErrorCotizacionCliente] codigo ["+codigoControl+"]");}
                if(!codigoControl.equalsIgnoreCase(codSrvCorrecto)){
                    return codigoControl;
                }
            }
        }catch(Exception ex){
            if( log.isEnabledFor(Level.ERROR) ){
                log.error("[CalculoRentaBean.validarErrorCotizacionCliente] Excepción = " + ex);
            }
            throw new Exception("Error al validar si Cotizacion Cliente tiene error.");
        }
        return TablaValores.getValor(ARCHIVO_PARAMETROS, "ERROR_PREVIRED", "errorGenerico");
    }



    /**
     * <p>Método que permite agrupar los datos de los detalles del error al momento de
     *    actualizar una renta de cliente.</p>
     *
     * Registro de Versiones:<ul>
     *
     * <LI>1.0  (22/04/2009 Pedro Carmona Escobar. (SEnTRA)): versión inicial.</li>
     *
     * </UL>
     * @param canal con el canal por el cual se hace la consulta.
     * @param rutCliente rut del cliente.
     * @param dvCliente digito verificador del rut.
     * @param idRegistro identificador de la solicitud de información a Previred.
     * @param estado estado con el cual se guardará el regsitro.
     * @return objeto con la información de la cotización realizada.
     * @throws Exception
     * @since 4.8
     */
    private CotizacionClienteVO cargarDatosCotizacionError(String canal, long rutCliente, char dvCliente, int identificadoRegistro, String estado) throws Exception{
        CotizacionClienteVO cotizacionCliente = null;
        try{
            Date fechaConsulta = new Date();
            cotizacionCliente = new CotizacionClienteVO();
            cotizacionCliente.setRutCliente(rutCliente);
            cotizacionCliente.setDvCliente(dvCliente);
            cotizacionCliente.setCanal(canal);
            String servicio = TablaValores.getValor(ARCHIVO_PARAMETROS, "CODIGO_SERVICIO", "avance_multicredito");
            cotizacionCliente.setServicio(servicio);
            cotizacionCliente.setFechaConsulta(fechaConsulta);
            cotizacionCliente.setFirma("");
            cotizacionCliente.setFolio("");
            cotizacionCliente.setEstadoError("");
            cotizacionCliente.setEstadoRenta(estado);
            cotizacionCliente.setIdentificador(identificadoRegistro);
            if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.cargarDatosCotizacionError] cotizacionCliente ["+cotizacionCliente+"]");}
        }catch(Exception ex){
            if( log.isEnabledFor(Level.ERROR) ){
                log.error("[CalculoRentaBean.cargarDatosCotizacionError] Excepcio = " + ex);
            }
            throw new Exception("Error al ingresar los datos en VO.");
        }
        return cotizacionCliente;
    }

    /**
     * <p>Método que permite agrupar los datos de los detalles del error al momento de
     * actualizar una renta de cliente considerando el nombre del servicio usado.</p>
     *
     * Registro de Versiones:<ul>
     *
     * <LI>1.0  (27/05/2016 Hernán Rodriguez. (TINet) - Oliver Hidalgo (Ing. Soft. BCI):): versión inicial.</li>
     *
     * </UL>
     * @param canal con el canal por el cual se hace la consulta.
     * @param rutCliente rut del cliente.
     * @param dvCliente digito verificador del rut.
     * @param identificadoRegistro identificador de la solicitud de información a Previred.
     * @param estado estado con el cual se guardará el regsitro.
     * @param codServicio codigo servicio usado.
	 * @return cotizacionCliente to base para registro de cotización.
     * @throws Exception en caso de existir error al crear el to.
     * @since 11.5
     */
    private CotizacionClienteVO cargarDatosCotizacionError(String canal, long rutCliente, char dvCliente,
        int identificadoRegistro, String estado, String codServicio) throws Exception {
        CotizacionClienteVO cotizacionCliente = null;
        try{
            Date fechaConsulta = new Date();
            cotizacionCliente = new CotizacionClienteVO();
            cotizacionCliente.setRutCliente(rutCliente);
            cotizacionCliente.setDvCliente(dvCliente);
            cotizacionCliente.setCanal(canal);
            String servicio = TablaValores.getValor(ARCHIVO_PARAMETROS, "CODIGO_SERVICIO", codServicio);
            cotizacionCliente.setServicio(servicio);
            cotizacionCliente.setFechaConsulta(fechaConsulta);
            cotizacionCliente.setFirma("");
            cotizacionCliente.setFolio("");
            cotizacionCliente.setEstadoError("");
            cotizacionCliente.setEstadoRenta(estado);
            cotizacionCliente.setIdentificador(identificadoRegistro);
            if (log.isDebugEnabled()) {
                log.debug(
                    "[CalculoRentaBean.cargarDatosCotizacionError] cotizacionCliente [" + cotizacionCliente + "]");
            }
        }
		catch(Exception ex){
            if( log.isEnabledFor(Level.ERROR) ){
                log.error("[CalculoRentaBean.cargarDatosCotizacionError] Excepcio = " + ex);
            }
            throw new Exception("Error al ingresar los datos en VO.");
        }
        return cotizacionCliente;
    }

    /**
     * <p>Método que permite generar el xml de entrada para consultar en previred</p>
     *
     * Registro de Versiones:<ul>
     *
     * <LI>1.0  (22/04/2009 Pedro Carmona Escobar (SEnTRA)): versión inicial.</li>
     *
     * </UL>
     *
     * @param rutCliente rut del cliente.
     * @param dvCliente digito verificador del rut.
     * @param idRegistro identificador de la solicitud de información a Previred.
     * @return String con el xml para consultar en previred.
     * @since 4.8
     */
    private String generarEntradaConsultaPrevired(long rutCliente, char dvCliente, int idRegistro) throws Exception {
        String xmlFinal=null;
        String rutClientePrevired = rutCliente+"-"+dvCliente;
        try {
            if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.generarEntradaConsultaPrevired] idRegistro ["+idRegistro+"]");}
            DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = dBuilder.parse(new File("tablas/previred/xml/entradaPrevired.xml"));
            if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.generarEntradaConsultaPrevired] doc ["+doc+"]");}
            doc.getDocumentElement().normalize();
            String tag = TablaValores.getValor(ARCHIVO_PARAMETROS, "TAG_XML_PREVIRED", "peticion");
            if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.generarEntradaConsultaPrevired] tag ["+tag+"]");}
            NodeList nodos = doc.getElementsByTagName(tag);
            int totalNodos=nodos.getLength();
            if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.generarEntradaConsultaPrevired] en total hay ["+totalNodos+"]");}
            for(int i=0; i<totalNodos;i++){
                Node nodoHijo = nodos.item(i);
                if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.generarEntradaConsultaPrevired] nodoHijo 0 ["+nodoHijo+"]");}
                if(nodoHijo.getNodeType()==Node.ELEMENT_NODE){
                    Element primerElemento = (Element)nodoHijo;
                    String codAutentica = TablaValores.getValor(ARCHIVO_PARAMETROS, "VALOR_XML_PREVIRED", "autentica");
                    String codServicio = TablaValores.getValor(ARCHIVO_PARAMETROS, "VALOR_XML_PREVIRED", "servicio");
                    String tipoPedido = TablaValores.getValor(ARCHIVO_PARAMETROS, "TAG_XML_PREVIRED", "tipo");
                    String strNombre = TablaValores.getValor(ARCHIVO_PARAMETROS, "TAG_XML_PREVIRED", "nombre");
                    String strValor = TablaValores.getValor(ARCHIVO_PARAMETROS, "TAG_XML_PREVIRED", "valor");
                    String strUsuario = TablaValores.getValor(ARCHIVO_PARAMETROS, "VALOR_XML_PREVIRED", "usr");
                    String strClave = TablaValores.getValor(ARCHIVO_PARAMETROS, "VALOR_XML_PREVIRED", "clave");
                    String strRut = TablaValores.getValor(ARCHIVO_PARAMETROS, "VALOR_XML_PREVIRED", "rut");
                    String codigoIdentificador = TablaValores.getValor(ARCHIVO_PARAMETROS, "VALOR_XML_PREVIRED", "codigo");                         
                    String usuarioConexion = TablaValores.getValor(ARCHIVO_PARAMETROS, "AUTENTICAR_PREVIRED", "usuario");
                    String claveConexion = TablaValores.getValor(ARCHIVO_PARAMETROS, "AUTENTICAR_PREVIRED", "clave");
                    if(codAutentica.equalsIgnoreCase(primerElemento.getAttribute(tipoPedido).toString())){
                        if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.generarEntradaConsultaPrevired] tipo ["+primerElemento.getAttribute(tipoPedido).toString()+"]");}
                        NodeList nodosNivelDos = primerElemento.getChildNodes();
                        int totalNodosNivelDos=nodosNivelDos.getLength();
                        if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.generarEntradaConsultaPrevired] en total hay N2["+totalNodosNivelDos+"]");}
                        for(int j=0; j<nodosNivelDos.getLength();j++){
                            String nodoObtenido=nodosNivelDos.item(j).toString();
                            if (StringUtil.cuentaOcurrencias(TAG_BASURA_1,nodoObtenido) <= 0 && StringUtil.cuentaOcurrencias(TAG_BASURA_2,nodoObtenido) <= 0){
                                if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.generarEntradaConsultaPrevired] nodoObtenido{"+j+"}:"+nodoObtenido);}
                                Element primerElemento2 = (Element)nodosNivelDos.item(j);
                                if(primerElemento2.getAttribute(strNombre).trim().equalsIgnoreCase(strUsuario)){
                                    primerElemento2.setAttribute(strValor,usuarioConexion);
                                }else if(primerElemento2.getAttribute(strNombre).trim().equalsIgnoreCase(strClave)){
                                    primerElemento2.setAttribute(strValor,claveConexion);
                                }
                            }
                        }
                    }else if(codServicio.equalsIgnoreCase(primerElemento.getAttribute(tipoPedido).toString())){
                        if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.generarEntradaConsultaPrevired] tipo ["+primerElemento.getAttribute(tipoPedido).toString()+"]");}
                        NodeList nodosNivelDos = primerElemento.getChildNodes();
                        int totalNodosNivelDos=nodosNivelDos.getLength();
                        if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.generarEntradaConsultaPrevired] en total hay N2["+totalNodosNivelDos+"]");}
                        for(int j=0; j<nodosNivelDos.getLength();j++){
                            String nodoObtenido=nodosNivelDos.item(j).toString();
                            if (StringUtil.cuentaOcurrencias(TAG_BASURA_1,nodoObtenido) <= 0 && StringUtil.cuentaOcurrencias(TAG_BASURA_2,nodoObtenido) <= 0){
                                if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.generarEntradaConsultaPrevired] nodoObtenido{"+j+"}:"+nodoObtenido);}
                                Element primerElemento2 = (Element)nodosNivelDos.item(j);
                                if(log.isDebugEnabled()) {
                                    log.debug("[CalculoRentaBean.generarEntradaConsultaPrevired] strNombre::"+strNombre);
                                    log.debug("[CalculoRentaBean.generarEntradaConsultaPrevired] valor::"+primerElemento2.getAttribute(strNombre).trim());
                                    log.debug("[CalculoRentaBean.generarEntradaConsultaPrevired] strRut::"+strRut);
                                    log.debug("[CalculoRentaBean.generarEntradaConsultaPrevired] codigoIdentificador::"+codigoIdentificador);
                                }
                                if(primerElemento2.getAttribute(strNombre).trim().equalsIgnoreCase(strRut)){
                                    if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.generarEntradaConsultaPrevired] seteo::"+rutClientePrevired+" a ::"+strValor);}
                                    primerElemento2.setAttribute(strValor,rutClientePrevired);
                                }else if(primerElemento2.getAttribute(strNombre).trim().equalsIgnoreCase(codigoIdentificador)){
                                    if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.generarEntradaConsultaPrevired] seteo::"+idRegistro+" a ::"+strValor);}
                                    primerElemento2.setAttribute(strValor,String.valueOf(idRegistro));
                                }
                            }
                        }
                    }
                }
            }
            xmlFinal=obtieneStringDesdeDocument(doc);
        } catch (Exception e) {
            if( log.isEnabledFor(Level.ERROR) ){
                log.error("[CalculoRentaBean.generarEntradaConsultaPrevired] Exception = "+e.toString());
            }
            xmlFinal = "";
        }
        if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.generarEntradaConsultaPrevired] entrada ["+xmlFinal+"]");}
        return xmlFinal;
    }

    /**
     * <p>Método que permite agrupar los datos asociados al encabezado de las cotizaciones
     * de cliente.</p>
     *
     * Registro de Versiones:<ul>
     *
     * <LI>1.0  (22/04/2008 Pedro Carmona Escobar (SEnTRA)): versión inicial.</li>
     * <li>1.1  (02/07/2010 Paulina Vera M.       (SEnTRA)): Se añade parametro 'codServicio' que indica el servicio por el cual se
     *                                                          actualiza la renta.</li>
     *
     * </UL>
     * @throws Exception
     * @param canal con el canal por el cual se hace la consulta.
     * @param rutCliente rut del cliente.
     * @param dvCliente digito verificador del rut.
     * @param codServicio, codigo servicio
     * @return objeto con la información de la cotización realizada.
     * @since 4.8
     */ 
    private CotizacionClienteVO cargarDatosEncabezadoCotizacion(String canal, long rutCliente, char dvCliente, String codServicio) throws Exception{
        CotizacionClienteVO cotizacionCliente = null;
        try{    
            Date fechaConsulta = new Date();
            cotizacionCliente = new CotizacionClienteVO();
            cotizacionCliente.setRutCliente(rutCliente);
            cotizacionCliente.setDvCliente(dvCliente);
            cotizacionCliente.setCanal(canal);
            String servicio = TablaValores.getValor(ARCHIVO_PARAMETROS, "CODIGO_SERVICIO", codServicio);
            cotizacionCliente.setServicio(servicio);
            cotizacionCliente.setEstadoError("");
            cotizacionCliente.setEstadoRenta("");
            cotizacionCliente.setFechaConsulta(fechaConsulta);
        }catch(Exception ex){
            if( log.isEnabledFor(Level.ERROR) ){
                log.error("[CalculoRentaBean.cargarDatosEncabezadoCotizacion] Excepcion =" + ex);            
            }
            throw new Exception("Error al ingresar los datos en VO.");
        }
        return cotizacionCliente;
    }



    /**
     * <p>Método que permite validar la renta para saber si es necesario consultar cotizaciones a Previred.</p>
     *
     * Registro de Versiones:<ul>
     *
     * <LI>1.0  (22/04/2009 Pedro Carmona Escobar. (SEnTRA)): versión inicial.</li>
     *
     * </UL>
     *
     * @param rentaActual con el valor de la renta (en M$) a validar.
     * @param valorUF con el valor de la UF.
     * @return representa respuesta a si se puede consultar previred.
     * @since 4.8
     */
    private boolean validarConsultaPrevired(double rentaActual, double valorUF) throws Exception
    {
        boolean consultarPrevired = false;
        try{
            rentaActual = rentaActual * 1000;
            if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.validarConsultaPrevired] : renta actual::["+rentaActual+"]");}
            if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.validarConsultaPrevired] : valor UF::["+valorUF+"]");}
            boolean entreHorarioPrevired = validarHorarioPrevired();
            if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.validarConsultaPrevired] : cumple horario::["+entreHorarioPrevired+"]");}
            if (entreHorarioPrevired) {
                if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.validarConsultaPrevired] : valor maximo::["+TablaValores.getValor(ARCHIVO_PARAMETROS, "VALOR_MAXIMO_RENTA_UF", "valor")+"]");}
                if (rentaActual<(Double.parseDouble(TablaValores.getValor(ARCHIVO_PARAMETROS, "VALOR_MAXIMO_RENTA_UF", "valor")))*valorUF) {
                    if(log.isDebugEnabled()) {log.debug("[CalculoRentaBean.validarConsultaPrevired] : Renta menor a lo establecido parametricamente");}
                    consultarPrevired= true;
                }
            }
        } catch (Exception ex) {
            if( log.isEnabledFor(Level.ERROR) ){
                log.error("[CalculoRentaBean.validarConsultaPrevired]::Excepcion : " + ex);
            }
            throw new Exception("Error al validar Consulta Previred.");
        }
        if(log.isDebugEnabled()){log.debug("[CalculoRentaBean.validarConsultaPrevired] : consultarPrevired VoF::["+consultarPrevired+"]");}
        return consultarPrevired;
    }


    /**
     * <p>Método que permite validar si se esta dentro del horario en el cual se puede
     * hacer consultas a previred.</p>
     *
     * Registro de Versiones:<ul>
     *
     * <LI>1.0  (22/04/2009 Pedro Carmona Escobar (SEnTRA)): versión inicial.</li>
     *
     * </UL>
     * @throws Exception
     * @return representa respuesta a si se puede consultar previred.
     * @since 4.8
     */
    private boolean validarHorarioPrevired() throws Exception {
        try {
            int HH_INICIO_PR= new Integer(TablaValores.getValor(ARCHIVO_PARAMETROS, "HH_INICIO_PR", "valor")).intValue();
            int MM_INICIO_PR = new Integer(TablaValores.getValor(ARCHIVO_PARAMETROS, "MM_INICIO_PR", "valor")).intValue();
            int SS_INICIO_PR = new Integer(TablaValores.getValor(ARCHIVO_PARAMETROS, "SS_INICIO_PR", "valor")).intValue();
            int HH_CIERRE_PR = new Integer(TablaValores.getValor(ARCHIVO_PARAMETROS, "HH_CIERRE_PR", "valor")).intValue();
            int MM_CIERRE_PR = new Integer(TablaValores.getValor(ARCHIVO_PARAMETROS, "MM_CIERRE_PR", "valor")).intValue();
            int SS_CIERRE_PR = new Integer(TablaValores.getValor(ARCHIVO_PARAMETROS, "SS_CIERRE_PR", "valor")).intValue();
            if(log.isDebugEnabled()) {
                log.debug("[CalculoRentaBean.validarConsultaPrevired] : hora inicio:"+HH_INICIO_PR);
                log.debug("[CalculoRentaBean.validarConsultaPrevired] : minutos inicio:"+MM_INICIO_PR);
                log.debug("[CalculoRentaBean.validarConsultaPrevired] : segundos inicio:"+SS_INICIO_PR);
                log.debug("[CalculoRentaBean.validarConsultaPrevired] : hora fin:"+HH_CIERRE_PR);
                log.debug("[CalculoRentaBean.validarConsultaPrevired] : minutos fin:"+MM_CIERRE_PR);
                log.debug("[CalculoRentaBean.validarConsultaPrevired] : segundos fin:"+SS_CIERRE_PR);           
            }
            Calendar horaDisponibilidadInicio = Calendar.getInstance();
            horaDisponibilidadInicio.set(Calendar.HOUR_OF_DAY, HH_INICIO_PR);
            horaDisponibilidadInicio.set(Calendar.MINUTE, MM_INICIO_PR);
            horaDisponibilidadInicio.set(Calendar.SECOND, SS_INICIO_PR);
            Calendar horaDisponibilidadFin = Calendar.getInstance();
            horaDisponibilidadFin.set(Calendar.HOUR_OF_DAY, HH_CIERRE_PR);
            horaDisponibilidadFin.set(Calendar.MINUTE, MM_CIERRE_PR);
            horaDisponibilidadFin.set(Calendar.SECOND, SS_CIERRE_PR);
            Date horaInicio = horaDisponibilidadInicio.getTime();
            Date horaFin    = horaDisponibilidadFin.getTime();
            Date horaActual = Calendar.getInstance().getTime();
            if(log.isDebugEnabled()) {
                log.debug("[CalculoRentaBean.validarConsultaPrevired] : horarioInicio:"+horaInicio);
                log.debug("[CalculoRentaBean.validarConsultaPrevired] : horaFin:"+horaFin);
                log.debug("[CalculoRentaBean.validarConsultaPrevired] : horaActual:"+horaActual);
                log.debug("[CalculoRentaBean.validarConsultaPrevired] : horaActual 2:"+new Date());
                log.debug("[CalculoRentaBean.validarConsultaPrevired] : validacion1:"+horaActual.after(horaInicio));
                log.debug("[CalculoRentaBean.validarConsultaPrevired] : validacion2:"+horaActual.before(horaFin));
            }
            if (horaActual.after(horaInicio) && horaActual.before(horaFin)){
                return true;
            } else {
                return false;
            }
        } catch (Exception ex) {
            if( log.isEnabledFor(Level.ERROR) ){
                log.error("[CalculoRentaBean.validarConsultaPrevired] :: Excepcion : " + ex);
            }
            throw new Exception("Error al validar horario previred.");
        }
    }




    /**
     * <p>Método que permite obtener el estado actual de actualización de la renta de un cliente.</p>
     *
     * Registro de Versiones:<ul>
     *
     * <LI>1.0  (22/04/2009 Pedro Carmona Escobar (SEnTRA)): versión inicial.</li>
     *
     * </UL>
     * @param rut con el rut del cliente.
     * @param dv con el digito verficador del rut.
     * @param multiEnvironment con datos para la consulta.
     * @throws Exception
     * @return representa respuesta a si se puede consultar previred.
     * @since 4.8
     */
    private boolean estadoRenta(long rut, char dv, MultiEnvironment multiEnvironment ) throws Exception {
        boolean estdoRenta = false;
        try{
            SvcMetodosForaneos    svcmf = new SvcMetodosForaneosImpl();
            if (log.isDebugEnabled()) {log.debug( "[CalculoRentaBean.estadoRenta]["+rut+"] Antes obtieneEstdoRenta");}
            estdoRenta = svcmf.obtieneEstdoRenta(multiEnvironment, rut, dv);
        } catch (Exception e){
            if( log.isEnabledFor(Level.ERROR) ){
                log.error( "[CalculoRentaBean.estadoRenta]["+rut+"] Excepcion de ejecucion (Exception): " + e.toString());
            }
            estdoRenta = false;
        }
        if (log.isDebugEnabled()) {log.debug( "[CalculoRentaBean.estadoRenta]["+rut+"] Despues obtieneEstdoRenta: "+ estdoRenta);}
        return estdoRenta;
    }




    /**
     * Método que permite formar un xml dentro de un string que contiene las cotizaciones del cliente.
     * 
     * <p>Para ello se realizan los siguientes pasos:
     * <ul>
     * <li> Se intenta obtener el último registro de cotizaciones desde B.D. que pertenezca al cliente y 
     * que esté dentro de una fecha de validez. En caso de no poder obtenerse por error, o por inexistencia, 
     * se asume que las cotizaciones serán obtenidas desde Previred. 
     * <li> Si se obtiene ese último registro válido, se analiza el estado de la renta dentro del mismo, 
     * y según este estado se determina desde donde se obtendrán las cotizaciones.
     * <li> Se genera String con xml de las cotizaciones del cliente, ya sean obtenidas desde Previred o desde BD.
     * </ul>
     *
     * <p>Registro de Versiones:<ul>
     * <LI>1.0  (22/04/2009 Pedro Carmona Escobar (SEnTRA)): versión inicial.</li>
     * <li>1.1 30/07/2014 Eduardo Villagrán Morales (Imagemaker): se mejora y normaliza log
     * </UL>
     *
     * @param canal con el canal por el cual se está realizando el proceso.
     * @param rut con el rut del cliente.
     * @param dv con el digito verficador del rut.
     * @throws Exception con motivo de la cancelación de la obtención de las cotizaciones.
     * @return CotizacionClienteVO que contiene toda la información de las cotizaciones.
     * @since 4.8
     */
    private CotizacionClienteVO obtenerCotizaciones(String canal, long rut, char dv) throws Exception {
        if (getLogger().isInfoEnabled()) {
            getLogger().info("[obtenerCotizaciones] [" + rut + "] [BCI_INI] canal=<" + canal + ">");
        }
        Calendar fechaValidez = Calendar.getInstance();
        fechaValidez.setTime(new Date());
        CotizacionClienteVO cotizacionCliente = null;
        boolean obtenerDesdePrevired = false;
        try{
            int diasValidez = Integer.parseInt(
                    TablaValores.getValor(ARCHIVO_PARAMETROS, "DIAS_VALIDEZ", "valor"));
            fechaValidez.add(Calendar.DAY_OF_MONTH, (diasValidez*-1));
            cotizacionCliente = obtieneUltimoRegistroValidoDeCotizaciones(rut, fechaValidez.getTime());
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("[[obtenerCotizaciones] [" + rut + "] cotizacionCliente=<" 
                        + cotizacionCliente + ">");
            }
        }
        catch (Exception e){
 		       if (getLogger().isEnabledFor(Level.WARN)) {
 		           getLogger().warn("[obtenerCotizaciones] [" + rut + "] [Exception] " + e.getMessage(), e);
            }
            obtenerDesdePrevired = true;
        }
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("[obtenerCotizaciones] [" + rut + "] cotizacionCliente=<" 
                    + cotizacionCliente + ">");
        }
        if (cotizacionCliente==null){
            obtenerDesdePrevired = true;
        } 
        else if (cotizacionCliente.getEstadoRenta().equalsIgnoreCase(
                TablaValores.getValor(ARCHIVO_PARAMETROS, "EST_RENTA_NOCALCULADA", "valor"))){
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[obtenerCotizaciones] [" + rut + "] [BCI_FINEX] Lanzando Exception. "
                        + "Cotizaciones actualizada en BD no pasan validaciones. No se puede procesar.");
            }
            throw  new Exception("Cotizaciones actualizada en BD no pasan validaciones. No se puede procesar.");
        } 
        else if (cotizacionCliente.getEstadoRenta().equalsIgnoreCase(
                TablaValores.getValor(ARCHIVO_PARAMETROS, "EST_RENTA_CALCULADA", "valor"))){
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[obtenerCotizaciones] [" + rut + "] [BCI_FINEX] lanzando exception. " 
                        + "Cotizaciones actualizadas en BD válidas pero no pasan filtro de cálculo. "
                        + "No se procesará.");
            }
            throw  new Exception(
                    "Cotizaciones actualizadas en BD válidas pero no pasan filtro de cálculo. No se procesará.");
        } 
        else if (cotizacionCliente.getEstadoRenta().equalsIgnoreCase(
                TablaValores.getValor(ARCHIVO_PARAMETROS, "EST_RENTA_PROBLEMAACTUALIZACION", "valor"))){
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("[obtenerCotizaciones] [" + rut 
                        + "] Cotizaciones actualizada en BD útil para calculo. Se irá a buscar los registros."); 
            }
            obtenerDesdePrevired = false;
        } 
        else if (cotizacionCliente.getEstadoRenta().equalsIgnoreCase(
                TablaValores.getValor(ARCHIVO_PARAMETROS, "EST_RENTA_PROBLEMAPREVIRED", "valor"))){
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("[obtenerCotizaciones] [" + rut 
                        + "] No hay cotizaciones por problemas con Previred. "
                        + "Se ira a buscar los registros Previred nuevamente."); 
            }
            obtenerDesdePrevired = true;
        }
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("[obtenerCotizaciones] [" + rut + "] obtenerDesdePrevired=<" 
                    + obtenerDesdePrevired + ">");
        }
        if (obtenerDesdePrevired) { 
            cotizacionCliente = new CotizacionClienteVO();
            cotizacionCliente.setObtenidoDesdePrevired(true);
        }
        else {
            String folio = cotizacionCliente.getFolio();
            cotizacionCliente.setObtenidoDesdePrevired(false);
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("[obtenerCotizaciones] [" + rut + "] folio=<" + folio + ">");
            }
            try {
                CalculoRentaDAO calculoRentaDAO = new CalculoRentaDAO();
                if(calculoRentaDAO.obtieneDetalleCotizacion(folio)!=null){
                cotizacionCliente.setDetalleCotizacion(calculoRentaDAO.obtieneDetalleCotizacion(folio));
                }
                else{
                	 cotizacionCliente = new CotizacionClienteVO();
                     cotizacionCliente.setObtenidoDesdePrevired(true);
                }
            }
            catch (Exception e){
 			   if (getLogger().isEnabledFor(Level.ERROR)) {
 			       getLogger().error("[obtenerCotizaciones] [" + rut + "] [Exception] [BCI_FIEX] "
 			               + e.getMessage(), e);
                }
                throw  new Exception("Error al obtener cotizaciones de DB.");
            }
        }
        if (getLogger().isInfoEnabled()) {
            getLogger().info("[obtenerCotizaciones] [" + rut + "] [BCI_FINOK] retorno=<" 
                    + cotizacionCliente + ">");
        }
        return cotizacionCliente;
    }

    /**
     * <p>Método que permite actualizar la renta del cliente, previa valdiación de su renta.</p>
     *
     * Registro de Versiones:<ul>
     *
     * <LI>1.0  (22/04/2009 Pedro Carmona Escobar (SEnTRA)): versión inicial.</li>
     *
     * </UL>
     * @param rut con el rut del cliente.
     * @param dv con el digito verificador del rut.
     * @param cotizacionCliente con al información de la cotizaciones del cliente.
     * @param rentaActualizada con la renta actualizada del cliente
     * @throws Exception
     * @return CotizacionClienteVO en donde se ha actualizado el atributo 'EstadoRenta' que indica el resultado de la actualización.
     * @since 4.8
     */
    private CotizacionClienteVO actualizarRentaCalculada(long rut, char dv, CotizacionClienteVO cotizacionCliente, double rentaActualizada ) throws Exception {

        ServiciosCliente servicioCliente = obtenerInstanciaSeriviciosCliente();
        if (log.isDebugEnabled()) {log.debug("[CalculoRentaBean.actualizarRentaCalculada] RentaActualizada RLM::"+rentaActualizada);}
        double valorMaxRentaUF = Double.parseDouble(TablaValores.getValor(ARCHIVO_PARAMETROS, "VALOR_MAXIMO_RENTA_UF", "valor"));
        if (log.isDebugEnabled()) {log.debug("[CalculoRentaBean.actualizarRentaCalculada] ValorMaxRentaUF::"+valorMaxRentaUF);}
        double valorUF = obtenerValorUF();
        if (log.isDebugEnabled()) {log.debug("[CalculoRentaBean.actualizarRentaCalculada] ValorUF::"+valorUF);}
        if(rentaActualizada < valorMaxRentaUF*valorUF){
            double rentaActual= 0;
            double proporcionRentaFijaActual = 0;
            boolean errorRentaActual = false;
            try{
                ClientePersona cl = servicioCliente.getClientePersona(rut,dv);
                if (log.isDebugEnabled()) {log.debug("[CalculoRentaBean.actualizarRentaCalculada] rentaFija: [" + cl.rentaFija + "] rentaVariable: [" + cl.rentaVariable + "]");}
                rentaActual = cl.rentaFija + cl.rentaVariable;
                if (rentaActual > 0) {
                    proporcionRentaFijaActual = (cl.rentaFija * 100) / rentaActual;
                } else {
                    proporcionRentaFijaActual = 100;
                }
                if (log.isDebugEnabled()) {log.debug("[CalculoRentaBean.actualizarRentaCalculada] proporcionRentaFijaActual: [" + proporcionRentaFijaActual + "]");}
                rentaActual = rentaActual * 1000;
            }catch(Exception ex){
                if( log.isEnabledFor(Level.ERROR) ){
                    log.error("[CalculoRentaBean.actualizarRentaCalculada] Exception al obtenerRentaClientePersona[" + ex.toString() + "]");
                }
                errorRentaActual = true;
            }
            if (log.isDebugEnabled()) {log.debug("[CalculoRentaBean.actualizarRentaCalculada] RentaActual::"+rentaActual);}
            if (log.isDebugEnabled()) {log.debug("[CalculoRentaBean.actualizarRentaCalculada] errorRentaActual::"+errorRentaActual);}
            double porcentajeRentaAnterior = Double.parseDouble(TablaValores.getValor(ARCHIVO_PARAMETROS, "PORCENTAJE_RENTA_ANTERIOR", "valor"));
            if (log.isDebugEnabled()) {log.debug("[CalculoRentaBean.actualizarRentaCalculada] porcentajeRentaAnterior::"+porcentajeRentaAnterior);}
            if(rentaActualizada >= porcentajeRentaAnterior*rentaActual && !errorRentaActual){
                Date periodoRentaVar = new Date();
                Date periodoRentaLiq = new Date();
                double rentaActualizadaFormateada = NumerosUtil.redondearAMiles(rentaActualizada);
                if (log.isDebugEnabled()) {log.debug("[CalculoRentaBean.actualizarRentaCalculada] rentaRedondeada::"+rentaActualizadaFormateada);}
                double rentaFijaActualizada = NumerosUtil.redondearAMiles((rentaActualizadaFormateada * proporcionRentaFijaActual) / 100);
                double rentaVariableActualizada = rentaActualizadaFormateada - rentaFijaActualizada;
                rentaFijaActualizada = rentaFijaActualizada/1000;
                rentaVariableActualizada = rentaVariableActualizada/1000;
                if (log.isDebugEnabled()) {log.debug("[CalculoRentaBean.actualizarRentaCalculada] renta Act. en Miles fija ["+rentaFijaActualizada+"]    variable ["+rentaVariableActualizada+"]");}
                Double rentaCalculada = new Double(rentaFijaActualizada);
                Double rentaVariable = new Double(rentaVariableActualizada);
                boolean actualizaRenta = false;
                try {
                    actualizaRenta = servicioCliente.actualizaClientePersonaRenta(new Long(rut), String.valueOf(dv), "", "", rentaVariable, periodoRentaVar, rentaCalculada, periodoRentaLiq);
                } catch (Exception e) {
                    if( log.isEnabledFor(Level.ERROR) ){
                        log.error("[CalculoRentaBean.actualizarRentaCalculada] Se produjo un error al actualizar renta(" + e.toString() + ")");
                    }
                    actualizaRenta = false;
                }
                if (log.isDebugEnabled()) {log.debug("[CalculoRentaBean.actualizarRentaCalculada] actualizaRenta ["+actualizaRenta+"]");}
                if(actualizaRenta){
                    if (log.isDebugEnabled()) {log.debug("[CalculoRentaBean.actualizarRentaCalculada] cotizacionCliente != NULL se actualizó renta.");}
                    cotizacionCliente.setEstadoRenta(TablaValores.getValor(ARCHIVO_PARAMETROS, "EST_RENTA_ACTUALIZADA", "valor"));
                }else{
                    if (log.isDebugEnabled()) {log.debug("[CalculoRentaBean.actualizarRentaCalculada] No se pudo actualizar renta por servicio.");}
                    cotizacionCliente.setEstadoRenta(TablaValores.getValor(ARCHIVO_PARAMETROS, "EST_RENTA_PROBLEMAACTUALIZACION", "valor"));
                }
            }else{
                if (log.isDebugEnabled()) {log.debug("[CalculoRentaBean.actualizarRentaCalculada] No se supera filtro rentaActual < porcentajeRentaAnterior *rentaAnterior.");}
                cotizacionCliente.setEstadoRenta(TablaValores.getValor(ARCHIVO_PARAMETROS, "EST_RENTA_CALCULADA", "valor"));
            }
        }else{
            if (log.isDebugEnabled()) {log.debug("[CalculoRentaBean.actualizarRentaCalculada] No se supera filtro rentaActualizada < valorMaxRentaUF*valorUF");}
            cotizacionCliente.setEstadoRenta(TablaValores.getValor(ARCHIVO_PARAMETROS, "EST_RENTA_CALCULADA", "valor"));
        }
        return cotizacionCliente;
    }




    /**
     * <p>Método que permite actualizar el estado de la actualización de renta de un cliente, reflejado en el
     * encabezado del registro de sus cotizaciones en BD.</p>
     *
     * Registro de versiones:<UL>
     *
     * <li>1.0 22/04/2009, Pedro Carmona E. (SEnTRA): versión inicial.</li>
     *
     * </UL>
     *
     * @param idRegistro con el identificador único del registro.
     * @param estado con el nuevo estado.
     * @return int que indica el resultado de la actualización.
     * @throws Exception
     * @since 1.1
     */ 
    private int actualizaEstadoEncabezado(int idRegistro, String estado) throws Exception{
        int resultado = 0;
        if (log.isDebugEnabled()) {log.debug("[CalculoRentaBean.actualizaEstadoEncabezado] En actualizaEstadoEncabezado");}
        try {
            CalculoRentaDAO calculoRentaDAO = new CalculoRentaDAO();
            if (log.isDebugEnabled()) {log.debug("[CalculoRentaBean.actualizaEstadoEncabezado] Luego de instanciar el DAO[" + calculoRentaDAO + "]"); }
            resultado = calculoRentaDAO.actualizaEstadoEncabezado(idRegistro, estado);
            if (log.isDebugEnabled()) {log.debug("[CalculoRentaBean.actualizaEstadoEncabezado] resultado ["+resultado+"]");}
        } catch (Exception ex) {
            if( log.isEnabledFor(Level.ERROR) ){
                log.error("[CalculoRentaBean.actualizaEstadoEncabezado]:Error [" + ex.toString() + "]");
            }
            throw ex;
        }
        return resultado;
    }




    /**
     * <p>Método que permite obtener el encabezado de las últimas contizaciones registradas en BD.</p>
     *
     * Registro de versiones:<UL>
     *
     * <li>1.0 22/04/2009, Pedro Carmona E. (SEnTRA): versión inicial.</li>
     *
     * </UL>
     * @param rut del cliente a consultar.
     * @param fechaValidez con fecha de validez.
     * @return cotizacionClienteVO con el último encabezado válido encontrado.
     * @throws Exception
     * @since 4.3
     */
    private CotizacionClienteVO obtieneUltimoRegistroValidoDeCotizaciones(long rut, Date fechaValidez) throws Exception {
        if (log.isDebugEnabled()) {log.debug("[CalculoRentaBean.obtieneUltimoRegistroValidoDeCotizaciones] rut ["+rut+"]   fechaValidez ["+fechaValidez+"]");}
        CotizacionClienteVO cotizacionCliente = null;
        try{
            CalculoRentaDAO calculoRentaDAO = new CalculoRentaDAO();
            if (log.isDebugEnabled()) {log.debug("[CalculoRentaBean.obtieneUltimoRegistroValidoDeCotizaciones]: Luego de instanciar el DAO[" + calculoRentaDAO + "]");}
            cotizacionCliente = calculoRentaDAO.obtieneUltimoRegistroValidoDeCotizaciones(rut, fechaValidez);
        } catch (Exception ex){
            if( log.isEnabledFor(Level.ERROR) ){
                log.error("[CalculoRentaBean.obtieneUltimoRegistroValidoDeCotizaciones] Excepción :" + ex.toString());
            }
            throw ex;
        }
        if (log.isDebugEnabled()) {log.debug("[CalculoRentaBean.obtieneUltimoRegistroValidoDeCotizaciones]: cotizacionCliente = " + cotizacionCliente);}
        return cotizacionCliente;
    }

    /**
     * Método que obtiene las rentas del cliente desde el servicio de previred con el sistema de autenticación
     * biométrico de huella y CI.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 08/08/2011 Cristoffer Morales L. (TINet): Versión inicial.
     * <li>1.1 15/11/2011 Felipe Rivera C. (TINet): Se retira la lógica asociada al cálculo de la renta del
     * cliente con el fin de mover la misma al método que realiza las validaciones correspondientes y que este
     * cálculo no se realice si es que las validaciones asociadas no se cumplen.
     * </ul>
     * <p>
     * 
     * @param canal con el canal por el cual se está realizando el proceso.
     * @param rut identiciador de rut del solicitante.
     * @param dv dígito verificador del rut del solicitante.
     * @param idAgenteAtencion identificador del ejecutivo.
     * @param datosBiometricos datos biométicos necesarios para la consulta de cotizaciones previciones
     * @return estructura con los datos de las rentas del cliente.
     * @throws GeneralException en caso de ocurrir un error durante la consulta
     * @since 5.10
     */
    public RentaSolicitanteTO obtenerRentaConCotizacionesPrevired(String canal, long rut, char dv,
            String idAgenteAtencion, DatosBiometricosTO datosBiometricos) throws GeneralException {
        if (log.isDebugEnabled()) {
            log.debug("[obtenerRentaConCotizacionesPrevired] canal [" + canal + "] rut [" + rut + "] dv [" + dv
                    + "]");
        }
        CotizacionClienteVO cotizacionCliente = null;
        DetalleCotizacionClienteVO[] detCotizCliente = null;
        RentaSolicitanteTO rentaSolicitante = null;
        int codigoRegistro = -1;
        AntecedentesPrevisionalesDAO antecedentesPrevisionalesDAO = new AntecedentesPrevisionalesDAO();
        log.debug("[obtenerRentaConCotizacionesPrevired] antes de realizar la consulta");
        rentaSolicitante = antecedentesPrevisionalesDAO.obtenerInformeCotizacionesPrevisionales(canal, rut, dv,
                idAgenteAtencion, datosBiometricos);
        log.debug("[obtenerRentaConCotizacionesPrevired] cotizaciones obtenidas");
        int codigoEjecucucionCorrecta = Integer.parseInt(TablaValores.getValor(
                SolicitudInstantaneaUtil.TABLA_SOLICITUD_INSTANTANEA, "VALIDACION_BIOMETRICA", "EJECUCION_OK"));

        if (rentaSolicitante != null) {
            log.debug("[obtenerRentaConCotizacionesPrevired] verificando codito de retorno del servicio");
            if (rentaSolicitante.getCodigoRetorno() != codigoEjecucucionCorrecta) {
                if (log.isDebugEnabled()) {
                    log
                    .debug("[obtenerRentaConCotizacionesPrevired] codigo de retorno corresponde a problema: "
                            + rentaSolicitante.getCodigoRetorno());
                }
                ErrorCotizacionVO errorCotizacion = new ErrorCotizacionVO();
                rentaSolicitante.setEstadoErrorRentas(true);
                String glosa = TablaValores.getValor(SolicitudInstantaneaUtil.TABLA_SOLICITUD_INSTANTANEA,
                        "CODIGO_RETORNO_WS_SINACOFI", String.valueOf(rentaSolicitante.getCodigoRetorno()));
                errorCotizacion.setCodigoError(String.valueOf(rentaSolicitante.getCodigoRetorno()));
                errorCotizacion.setDetalleError(glosa);
                rentaSolicitante.setErrorCotizacionVO(errorCotizacion);
            }
            else {
                log.debug("[obtenerRentaConCotizacionesPrevired] codigo de retorno OK");
                try {
                    log.debug("[obtenerRentaConCotizacionesPrevired] obteniendo datos principales cotización");
                    cotizacionCliente = SolicitudInstantaneaUtil.cargarDatosCotizacionPrevired(rut, dv, canal,
                            rentaSolicitante.getXmlCotizaciones(), codigoRegistro);
                    log.debug("[obtenerRentaConCotizacionesPrevired] obteniendo detalle de cotizaciones");
                    detCotizCliente = SolicitudInstantaneaUtil
                            .cargarDatosDetalleCotizacionPrevired(rentaSolicitante.getXmlCotizaciones());
                    cotizacionCliente.setDetalleCotizacion(detCotizCliente);

                    rentaSolicitante.setEstadoErrorRentas(false);
                    rentaSolicitante.setCotizacionClienteVO(cotizacionCliente);
                    log
                    .debug("[obtenerRentaConCotizacionesPrevired] fin obtencion detalle y calculo renta");
                }
                catch (GeneralException ge) {
                    if (log.isEnabledFor(Level.ERROR)) {
                        log.error(
                                "Ha ocurrido un error durante el proceso de obteción detalle y calculo de renta", ge);
                    }
                    throw ge;
                }
            }
        }
        return rentaSolicitante;
    }

    /**
     * <p>
     * Método encargado de llamar al evaluador de reglas y retornar solo el
     * objeto de respuesta con el calculo realizado.
     * </p>
     * 
     * Registro de versiones:
     * <ul>
     * <li>1.0 29-04-2013 Samuel Merino A. (ADA): versión inicial</li>
     * </ul>
     * <p>
     * 
     * @param rentaSolicitanteTO objeto que contiene las cotizaciones y datos del cliente.
     * @param isClienteVigente indicador que señala el estado del cliente (True o False).
     * @return ReglaRentaTO objeto que contiene respuesta desde el evaluador y cálculo.
     * @since 1.0
     */
    public ReglaRentaTO evaluarReglas(RentaSolicitanteTO rentaSolicitanteTO, boolean isClienteVigente) {

        if (log.isDebugEnabled()) {
            log.debug("[CalculoRentaBean][evaluarReglas] Inicio metodo!!");
            log.debug("[CalculoRentaBean][evaluarReglas] rentaSolicitanteTO:"
                    + StringUtil.contenidoDe(rentaSolicitanteTO));
            log.debug("[CalculoRentaBean][evaluarReglas] isClienteVigente:" + isClienteVigente);

        }

        DetalleCotizacionClienteVO[] detalleCotizacionesClienteVO = null;
        ReglaRentaTO[] reglaRentaTO = null;
        ReglaRentaTO reglaRentaRetorno = null;

        String reglas = TablaValores.getValor(TABLA_PARAMETROS, "REGLAS", "param");

        if (log.isDebugEnabled()) {
            log.debug("[CalculoRentaBean][evaluarReglas] reglas:" + reglas);
            log.debug("[CalculoRentaBean][evaluarReglas] llamada a filtrarListadoDeCotizaciones");
        }

        detalleCotizacionesClienteVO = SolicitudInstantaneaUtil.filtrarListadoDeCotizaciones(rentaSolicitanteTO
                .getCotizacionClienteVO().getDetalleCotizacion());

        if (log.isDebugEnabled()) {
            log.debug("[CalculoRentaBean][evaluarReglas] llamada exitosa");
            log.debug("[CalculoRentaBean][evaluarReglas] detalleCotizacionesClienteVO:"
                    + StringUtil.contenidoDe(detalleCotizacionesClienteVO));
            log.debug("[CalculoRentaBean][evaluarReglas] instanciando evaluador de renta");
        }

        EvaluadorReglasDeRenta evaluadorReglasDeRenta = new EvaluadorReglasDeRenta(reglas, rentaSolicitanteTO);

        if (log.isDebugEnabled()) {
            log.debug("[CalculoRentaBean][evaluarReglas] llamada a evaluarReglas");
        }

        reglaRentaTO = evaluadorReglasDeRenta.evaluarReglas(isClienteVigente);
        if (log.isDebugEnabled()) {
            log.debug("[CalculoRentaBean][evaluarReglas] llamada exitosa");
            log.debug("[CalculoRentaBean][evaluarReglas] reglaRentaTO:" + StringUtil.contenidoDe(reglaRentaTO));
        }

        if (reglaRentaTO != null && reglaRentaTO.length > 0) {

            if (log.isDebugEnabled()) {
                log.debug("[CalculoRentaBean][evaluarReglas] reglaRentaTO NO es nulo");
            }

            for (int i = 0; i < reglaRentaTO.length; i++) {

                if (!reglaRentaTO[i].isCumpleRegla() && reglaRentaTO[i].isFalloDetieneEjecucion()) {

                    reglaRentaTO[i].setRentaCalculada(0);

                    if (log.isDebugEnabled()) {
                        log.debug("[CalculoRentaBean][evaluarReglas] renta calculada con valor 0");
                        log.debug("[CalculoRentaBean][evaluarReglas] retornando");
                    }


                    reglaRentaRetorno = reglaRentaTO[i];
                    if (log.isDebugEnabled()) {
                        log.debug("[CalculoRentaBean][evaluarReglas] reglaRentaRetorno:"
                                + StringUtil.contenidoDe(reglaRentaRetorno));
                    }
                    return reglaRentaRetorno;
                }

                if (reglaRentaTO[i].getIdRenta() == IdentificadorReglaRentaTO.CALCULAR_RENTA) {

                    if (log.isDebugEnabled()) {
                        log.debug("[CalculoRentaBean][evaluarReglas] identificador renta con valor 3");
                        log.debug("[CalculoRentaBean][evaluarReglas] retornando");
                    }

                    reglaRentaRetorno = reglaRentaTO[i];
                    if (log.isDebugEnabled()) {
                        log.debug("[CalculoRentaBean][evaluarReglas] reglaRentaRetorno:"
                                + StringUtil.contenidoDe(reglaRentaRetorno));
                    }
                    return reglaRentaRetorno;
                }
            } 
        }

        if (log.isDebugEnabled()) {
            log.debug("[CalculoRentaBean][evaluarReglas] reglaRentaRetorno:"
                    + StringUtil.contenidoDe(reglaRentaRetorno));
        }
        return reglaRentaRetorno;

    }
    
    
    
    
    /**
     * <p>
     * Método encargado de llamar al evaluador de reglas para cotizaciones obtenidas desde Equifax, y retornar
     *  sólo el objeto de respuesta con el calculo realizado.
     * </p>
     * 
     * Registro de versiones:
     * <ul>
     * <li>1.0 16/09/2014 Pedro Carmona Escobar (SEnTRA): versión inicial</li>
     * </ul>
     * <p>
     * 
     * @param rentaSolicitanteTO objeto que contiene las cotizaciones y datos del cliente.
     * @param isClienteVigente indicador que señala el estado del cliente (True o False).
     * @return ReglaRentaTO objeto que contiene respuesta desde el evaluador y cálculo.
     * @since 7.0
     */
    public ReglaRentaTO evaluarReglasEquifax(RentaSolicitanteTO rentaSolicitanteTO, boolean isClienteVigente) {

        if (getLogger().isEnabledFor(Level.INFO)) {
            getLogger().info("[evaluarReglasEquifax][BCI_INI] rentaSolicitanteTO:" + rentaSolicitanteTO
                    + "  isClienteVigente:" + isClienteVigente);
        }

        DetalleCotizacionClienteVO[] detalleCotizacionesClienteVO = null;
        ReglaRentaTO[] reglaRentaTO = null;
        ReglaRentaTO reglaRentaRetorno = null;

        String reglas = TablaValores.getValor(TABLA_PARAMETROS, "REGLAS_EQUIFAX", "param");

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("[evaluarReglasEquifax] reglas:" + reglas);
        }

        detalleCotizacionesClienteVO = SolicitudInstantaneaUtil.filtrarListadoDeCotizacionesEquifax(
                rentaSolicitanteTO.getCotizacionClienteVO().getDetalleCotizacion());

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("[evaluarReglasEquifax] detalleCotizacionesClienteVO:"
                    + StringUtil.contenidoDe(detalleCotizacionesClienteVO));
        }

        EvaluadorReglasDeRenta evaluadorReglasDeRenta = new EvaluadorReglasDeRenta(reglas, rentaSolicitanteTO);

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("[evaluarReglasEquifax] evaluadorReglasDeRenta: " + evaluadorReglasDeRenta);
        }

        reglaRentaTO = evaluadorReglasDeRenta.evaluarReglas(isClienteVigente);
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("[evaluarReglasEquifax] reglaRentaTO:" + StringUtil.contenidoDe(reglaRentaTO));
        }

        if (reglaRentaTO != null && reglaRentaTO.length > 0) {
            for (int i = 0; i < reglaRentaTO.length; i++) {
                if (!reglaRentaTO[i].isCumpleRegla() && reglaRentaTO[i].isFalloDetieneEjecucion()) {
                    reglaRentaTO[i].setRentaCalculada(0);
                    reglaRentaRetorno = reglaRentaTO[i];
                    break;
                }
                if (reglaRentaTO[i].getIdRenta() == IdentificadorReglaRentaTO.CALCULAR_RENTA_EQUIFAX) {
                    reglaRentaRetorno = reglaRentaTO[i];
                    break;
                }
            } 
        }

        if (getLogger().isEnabledFor(Level.INFO)) {
            getLogger().info("[evaluarReglasEquifax][BCI_FINOK] reglaRentaRetorno:"
                    + StringUtil.contenidoDe(reglaRentaRetorno));
        }
        return reglaRentaRetorno;

    }

    /**
     * Método utilizado para actualizar la renta con el valor calculado por la regla de cálculo de renta, 
     * además se valida si la renta calculada se considera como fija o variable realizando la actualización sólo 
     * si corresponde hacerlo.
     * <p>
     * 
     * Registro de versiones:
     * <ul>
     * <li>1.0 30-04-2013 Samuel Merino A. (ADA): versión inicial</li>
     * <li>1.2 25/08/2014 Eduardo Villagrán Morales (Imagemaker): Se aplica cambio para corregir error de 
     *          NullPointerException. Se normaliza log.
     * <li>1.3 22-09-2014 Manuel Escárate (BEE): Se agrega usuario para la PL60, se aplican filtros
     *                    para setear fecha y renta o solo fecha, se agrega atributo aplicacion.
     *                    Se modifica el valor que se obtiene para la variable topeUf.
     * </ul>
     * <p>
     * 
     * @param rut del cliente.
     * @param dv  del cliente.
     * @param rentaSolicitanteTO objeto que contiene las rentas del cliente.
     * @param reglaRentaTO objeto que contiene la renta calculada del cliente.
     * @param isClienteVigente indicador que señala el estado del cliente.
     * @param aplicacion flujo de la actualización.
     * @throws GeneralException en caso de error se lanza esta exception.
     * @return CotizacionClienteVO objeto que contiene valores referente a la actualización de renta.
     * @since 1.0
     */
    public CotizacionClienteVO actualizarRentaCalculada(long rut, char dv, RentaSolicitanteTO rentaSolicitanteTO,
            ReglaRentaTO reglaRentaTO, boolean isClienteVigente,String aplicacion) throws GeneralException {

        if (getLogger().isInfoEnabled()){
            getLogger().info("[actualizarRentaCalculada] [" + rut + "] [BCI_INI] rentaSolicitanteTO=<"
                    + StringUtil.contenidoDe(rentaSolicitanteTO) + ">, reglaRentaTo=<" 
                    + StringUtil.contenidoDe(reglaRentaTO) + ">, isClienteVigente=<" + isClienteVigente + ">");
        }

        CotizacionClienteVO cotizacionCliente = rentaSolicitanteTO.getCotizacionClienteVO();

        String ultimoPeriodoAct =rentaSolicitanteTO.getCotizacionClienteVO().getDetalleCotizacion()[0]
                .getPeriodo();
        if (getLogger().isInfoEnabled()) {
            getLogger().info("[actualizarRentaCalculada] [" + rut 
                        + "] ultimoPeriodoActasd=<" + ultimoPeriodoAct + ">");
            }
   
        double variabilidad = 0;
        String factorVariabilidad = null;
        Date fechaHoy = new Date();
        String topeUf = TablaValores.getValor(TABLA_PARAMETROS_CALCULORENTA,
                "rentaTope","renta");
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("[actualizarRentaCalculada] [" + rut + "] topeUf=<" + topeUf + ">, fechaHoy=<" 
                    + fechaHoy + ">");
        }
        double valorUf = WCorpUtils.getValorUF(fechaHoy);
        if (getLogger().isInfoEnabled()) {
            getLogger().info("[actualizarRentaCalculada] [" + rut + "] valorUf=<" + valorUf + ">");
            }

            double topePeso = Double.parseDouble(topeUf) * valorUf;
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("[actualizarRentaCalculada] [" + rut + "] topePeso=<" + topePeso + ">");
            }
            double ultimaRenta = 0;

                if (rentaSolicitanteTO.getCotizacionClienteVO() != null) {
                    if (rentaSolicitanteTO.getCotizacionClienteVO().getDetalleCotizacion()[0] != null) {
                        ultimaRenta = rentaSolicitanteTO.getCotizacionClienteVO().getDetalleCotizacion()[0]
                                .getRemuneracionImponible();
                    }
                }

            if (getLogger().isInfoEnabled()) {
                getLogger().info("[actualizarRentaCalculada] [" + rut + "] ultimaRenta=<" + ultimaRenta + ">");
            }

            if (ultimaRenta < topePeso) {

                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("[actualizarRentaCalculada] [" + rut 
                            + "] ultimaRenta menor al tope en pesos");
                }

                variabilidad = SolicitudInstantaneaUtil.obtenerVariabilidadRentasPrevired(rentaSolicitanteTO
                        .getCotizacionClienteVO().getDetalleCotizacion());
                factorVariabilidad = TablaValores.getValor(TABLA_PARAMETROS, "VAL_PREVIRED",
                        "comparadorVariabilidad");
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("[actualizarRentaCalculada] [" + rut+ "] variabilidad=<" + variabilidad 
                            + ">, factorVariabilidad=<" + factorVariabilidad + ">");
                }

                if (variabilidad > Double.parseDouble(factorVariabilidad)) {
                    cotizacionCliente.setMontoRentaVariable(reglaRentaTO.getRentaCalculada());
                } 
                else {
                    cotizacionCliente.setMontoRentaFija(reglaRentaTO.getRentaCalculada());
                }
            }
            if (ultimaRenta >= topePeso) {

                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("[actualizarRentaCalculada] [" + rut 
                            + "] ultimaRenta mayor o igual topePeso");
                }

                double rentaActual = rentaSolicitanteTO.getRentaFija() + rentaSolicitanteTO.getRentaVariable();
                double rentaCalculadaPrevired = reglaRentaTO.getRentaCalculada();

                if (getLogger().isInfoEnabled()) {
                    getLogger().info("[actualizarRentaCalculada] [" + rut + "] rentaActual=<" + rentaActual 
                            +">, rentaCalculadaPrevired=<" + rentaCalculadaPrevired + ">");
                }

                if (rentaActual > rentaCalculadaPrevired) {
                    if (getLogger().isInfoEnabled()) {
                        getLogger().info("[actualizarRentaCalculada] [" + rut 
                                + "] rentaActual mayor a rentaCalculadaPrevired. Se mantienen valores de renta y "
                                + "fuente de ingreso EstimacionDeRentaPrevired");
                    }
                } 
                else {

                    if (getLogger().isInfoEnabled()) {
                        getLogger().info("[actualizarRentaCalculada] [" + rut 
                                + "] rentaActual menor a rentaCalculadaPrevired");
                    }

                    variabilidad = SolicitudInstantaneaUtil.obtenerVariabilidadRentasPrevired(rentaSolicitanteTO
                            .getCotizacionClienteVO().getDetalleCotizacion());
                    factorVariabilidad = TablaValores.getValor(TABLA_PARAMETROS, "VAL_PREVIRED",
                            "comparadorVariabilidad");
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("[actualizarRentaCalculada] [" + rut + "] variabilidad=<" 
                                + variabilidad + ">, factorVariabilidad=<" + factorVariabilidad + ">");
                    }

                    if (variabilidad > Double.parseDouble(factorVariabilidad)) {
                        cotizacionCliente.setMontoRentaVariable(reglaRentaTO.getRentaCalculada());
                    } 
                    else {
                        cotizacionCliente.setMontoRentaFija(reglaRentaTO.getRentaCalculada());
                    }
                }
            }

        boolean actualizaClientePersona = false;
        try {

            ServiciosCliente servicioCliente;
            servicioCliente = obtenerInstanciaSeriviciosCliente();

            String codMov = TablaValores.getValor(TABLA_PARAMETROS_CALCULORENTA, "RentaEstimadaPrevired", "valor");
            String codUsuarioAct = TablaValores
                    .getValor(TABLA_PARAMETROS_CALCULORENTA, "usuarioMod", "valor");

            if (getLogger().isDebugEnabled()) {
                getLogger().debug("[actualizarRentaCalculada] [" + rut + "] codMov=<" + codMov + ">");
            }

            if (getLogger().isDebugEnabled()) {
                getLogger().debug("[actualizarRentaCalculada] [" 
                 + rut + "] codUsuarioAct=<" + codUsuarioAct + ">");
            }

            Date periodoRentaVar = new Date();
            Date periodoRentaLiq = new Date();

            double cotizacionesSumadas = cotizacionCliente.getMontoRentaFija()
                        +cotizacionCliente.getMontoRentaVariable();
            double cotizacionesDivPrevired = cotizacionesSumadas/MONTO_MULTI_RENTA;
            double rentaPrevired = DoubleUtl.redondea(cotizacionesDivPrevired ,0)*MONTO_MULTI_RENTA;
            
            if (getLogger().isInfoEnabled()){
                  getLogger().info("[actualizarRentaCalculada] [" 
                              + rut + "] rentaPrevired=<" + rentaPrevired + ">");
            }
            String miPerfil = TablaValores.getValor(TABLA_PARAMETROS_CALCULORENTA,
                    "actualizacionRentaMiPerfil","aplicacion");
            String simulador = TablaValores.getValor(TABLA_PARAMETROS_CALCULORENTA,
                    "actualizacionRentaSimulador","aplicacion");
            String strValor = TablaValores.getValor(
                    ARCHIVO_PARAMETROS, "OMITIR_ACTUALIZAR_RENTA", "valor");
            boolean omitirActualizarRenta = strValor==null ? false : strValor.equalsIgnoreCase("ON");
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("[actualizarRentaCalculada] [" + rut + "] strValor=<" + strValor 
                        + ">, omitirActualizarRenta=<" + omitirActualizarRenta + ">");
            }
            if (aplicacion != null && (aplicacion.equalsIgnoreCase(miPerfil) 
                    || aplicacion.equalsIgnoreCase(simulador))){
                double montoTope =  Double.parseDouble(TablaValores.getValor(TABLA_PARAMETROS_CALCULORENTA,
                        "rentaTope","renta"));
                double valorUF = obtenerValorUF();
                double montoTopeTotal = montoTope * valorUF;
                double rentaLiquidaCliente =((rentaSolicitanteTO.getRentaFija()
                        +rentaSolicitanteTO.getRentaVariable())*MONTO_MULTI_RENTA);
                double rentaPreviredParaActualizar = DoubleUtl.redondea(
                        (cotizacionCliente.getMontoRentaFija()/(double)MONTO_MULTI_RENTA),0);    
                double rentaVariableParaActualizar = DoubleUtl.redondea(
                        (cotizacionCliente.getMontoRentaVariable()/(double)MONTO_MULTI_RENTA),0);   
                double montoTopeNuevo = Double.parseDouble(TablaValores.getValor(
                        "CalculoRenta.parametros", "topeNuevo", "tope"));
                double montoRentaFija = 0;
                double montoRentaVariable = 0;
                
                boolean actualizaSGC = false;
                  if (rentaSolicitanteTO != null) {
                      if ((rentaLiquidaCliente > montoTopeTotal) && (rentaLiquidaCliente > montoTopeNuevo) 
                              && (rentaPrevired == montoTopeTotal)){
                          actualizaSGC = false;
                      }
                      if ((rentaLiquidaCliente > montoTopeTotal) && (rentaLiquidaCliente <= montoTopeNuevo) 
                            && (rentaPrevired == montoTopeTotal)){
                    	    montoRentaFija =rentaSolicitanteTO.getRentaFija();
                    	    montoRentaVariable =rentaSolicitanteTO.getRentaVariable();
                          actualizaSGC = true;
                      }
                    if ((rentaLiquidaCliente > montoTopeTotal) && (rentaPrevired < montoTopeTotal)){
                         actualizaSGC = false;
                    }
                    if ((rentaLiquidaCliente <= montoTopeTotal) && (rentaPrevired >= rentaLiquidaCliente)){
                    	montoRentaFija =rentaPreviredParaActualizar;
                    	montoRentaVariable = rentaVariableParaActualizar;
                        periodoRentaVar = FechasUtil.convierteStringADate(ultimoPeriodoAct,
                                 new SimpleDateFormat("MMyyyy"));
                        periodoRentaLiq = FechasUtil.convierteStringADate(ultimoPeriodoAct,
                                new SimpleDateFormat("MMyyyy"));
                        actualizaSGC = true;
                    }
                    if ((rentaLiquidaCliente <= montoTopeTotal) && (rentaPrevired < rentaLiquidaCliente)){
                        actualizaSGC = false;
                    }
                }
            if (omitirActualizarRenta){
                actualizaClientePersona = true;
            }
            else{
                       if (actualizaSGC){
            actualizaClientePersona = servicioCliente.actualizaClientePersonaRenta(new Long(rut), 
                               String.valueOf(dv), "", "", new Double(montoRentaVariable), 
                               periodoRentaVar, new Double(montoRentaFija)
                       , periodoRentaLiq, codMov,codUsuarioAct);
                       }
                       else {
                           actualizaClientePersona = true;
                       }
                   }  
            }
        } 
        catch (Exception e) {
            if(getLogger().isEnabledFor(Level.WARN)){
                getLogger().warn("[actualizarRentaCalculada] [" + rut + "] [Exception] " + e.getMessage(), e);
            }
            actualizaClientePersona = false;
        }

        if (getLogger().isInfoEnabled()){
            getLogger().info("[actualizarRentaCalculada] [" + rut + "] actualizaClientePersona=<"
                    + actualizaClientePersona + ">, cotizacionCliente=<" 
                    + StringUtil.contenidoDe(cotizacionCliente) + ">");
        }

        if (actualizaClientePersona) {
            cotizacionCliente.setEstadoRenta(TablaValores.getValor(
                    ARCHIVO_PARAMETROS, "EST_RENTA_ACTUALIZADA","valor"));
        } 
        else {
            cotizacionCliente.setEstadoRenta(TablaValores.getValor(
                    ARCHIVO_PARAMETROS, "EST_RENTA_PROBLEMAACTUALIZACION", "valor"));
            }
        if (getLogger().isInfoEnabled()){
            getLogger().info("[actualizarRentaCalculada] [" + rut + "] cotizacionCliente.getEstadoRenta()=<"
                    + cotizacionCliente.getEstadoRenta() + ">");
        }
        if (getLogger().isInfoEnabled()){
            getLogger().info("[actualizarRentaCalculada] [" + rut + "] [BCI_FINOK] retorno=<" 
                    + cotizacionCliente + ">");
        }
        return cotizacionCliente;
    }

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
     */
    public RentaCliente consultaUltimoCalculo(String rutCliente, String dvRut) throws Exception, RemoteException{
        RentaCliente rentaCliente = null;
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("[CalculoRentaBean.consultaUltimoCalculo] En consultaUltimoCalculo");
        }
        try {
            rentaCliente = trxBean.consultaUltimoCalculo(rutCliente, dvRut);
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("[CalculoRentaBean.consultaUltimoCalculo] resultado []");
            }
        }
        catch (Exception ex) {
            if( getLogger().isEnabledFor(Level.ERROR) ){
                getLogger().error("[CalculoRentaBean.consultaUltimoCalculo]:Error [" + ex.toString() + "]");
            }
            throw ex;
        }
        return rentaCliente;
    }

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
     * @since 6.0
     */
    public DetalleArriendoTO[] consultarDetalleBienRaiz(long rut, char dv, int secuencia) throws Exception{
        if(getLogger().isEnabledFor(Level.INFO)){
            getLogger().info("[consultarDetalleBienRaiz] Inicio Método con [rut]: ["+rut+"] [dv]: [" +dv+ "]" 
                + "[secuencia] : [" +secuencia+ "]");
        }
        try{
            CalculoRentaDAO calculoRentaDAO = new CalculoRentaDAO();
            if(getLogger().isEnabledFor(Level.DEBUG)){
                getLogger().debug("[ingresarDetalleBienRaiz] Después de instanciar DAO");
            }
            return calculoRentaDAO.consultarDetalleBienRaiz(rut, dv, secuencia);
        }
        catch (Exception e){
            if(getLogger().isEnabledFor(Level.ERROR)){
                getLogger().error("[consultarDetalleBienRaiz] Error en consulta de detalle bien raíz : " +e);
            }
            throw e;            
        }        
    }
    
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
     * @since 6.0
     */
    public boolean ingresarDetalleBienRaiz(long rut, char dv, int secuencia, DetalleArriendoTO detalleArriendoTO) 
            throws Exception{
        
        if(getLogger().isEnabledFor(Level.INFO)){
            getLogger().info("[ingresarDetalleBienRaiz] Inicio Método con [rut]: ["+rut+"] [dv]: [" +dv+ "]" 
                   + "[secuencia] : [" +secuencia+ "] [DetalleArriendoTO] : [" +detalleArriendoTO+ "]");
        }
        boolean respuesta = false;
        try{
            CalculoRentaDAO calculoDAO = new CalculoRentaDAO();
            if(getLogger().isEnabledFor(Level.DEBUG)){
                getLogger().debug("[ingresarDetalleBienRaiz] Después de instanciar DAO");
            }
            respuesta = calculoDAO.ingresarDetalleBienRaiz(rut, dv, secuencia, detalleArriendoTO);
            if(getLogger().isEnabledFor(Level.DEBUG)){
                getLogger().debug("[ingresarDetalleBienRaiz] Resultado de la operación de ingreso: " +respuesta);
            }
            
        }
        catch (Exception e){
            if(getLogger().isEnabledFor(Level.ERROR)){
                getLogger().error("[ingresoDetalleBienRaiz] Error en ingreso de detalle bien raíz : " +e);
            }
            throw e;
        }
        return respuesta;
        
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
     * @since 6.0
     */       
    public Logger getLogger(){
        if (log == null){
            log = Logger.getLogger(this.getClass());
        }
        return log;
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
     * @since 6.0
     */
    public void eliminarBorradorRenta(CalculoRentaTO calculoRenta) throws Exception{
        if (getLogger().isDebugEnabled()){
            getLogger().debug("[eliminarBorradorRenta][BCI_INI]");
        }
        try{
            CalculoRentaDAO dao = new CalculoRentaDAO();
            dao.eliminarBorradorRenta(calculoRenta);
            if (getLogger().isDebugEnabled()){
                getLogger().debug("[eliminarBorradorRenta][BCI_FINOK]");
            }
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
     * Método que obtiene las rentas del cliente mediante un cálculo realizado con información de las
     * cotizaciones previsionales obtenidas desde Equifax.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 16/09/2014 Pedro Carmona Escobar (SEnTRA): versión inicial.
     * 
     * <li>1.1 19/03/2015 Pedro Carmona Escobar (SEnTRA) - Andrés Alvarado (Ing. Soft. BCI): Se realiza modificación para que la renta imponible
     *                      para el cálculo de variablidad sea la suma de las cotizaciones en ese período.
     *                      Además, se realiza llamado a método particular para asignar de forma definida
     *                      procentajes de la renta a la parte varíable como fija.
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
     * @throws GeneralException en caso de ocurrir un error durante la consulta.
     * @since 7.0
     */
    public RentaSolicitanteTO obtenerRentaConCotizacionesEquifax(String canal, long rut, char dv,
            String idAgenteAtencion, String afp, String folioValidacion, Date fechaCertificado)
                    throws GeneralException {
        if (getLogger().isEnabledFor(Level.INFO)) {
            getLogger().info("[obtenerRentaConCotizacionesEquifax][" + rut + "][BCI_INI]");
        }
        if (getLogger().isEnabledFor(Level.DEBUG)) {
            getLogger().debug("[obtenerRentaConCotizacionesEquifax][" + rut + "] canal [" + canal
                    + "] dv [" + dv + "] idAgenteAtencion [" + idAgenteAtencion + "] afp [" + afp
                    + "] folioValidacion [" + folioValidacion + "] fechaCertificado [" + fechaCertificado + "]");
        }
        try {
            RentaSolicitanteTO rentaSolicitante = null;
            AntecedentesPrevisionalesDAO antecedentesPrevisionalesDAO = new AntecedentesPrevisionalesDAO();
            if (getLogger().isEnabledFor(Level.DEBUG)) {
                getLogger().debug("[obtenerRentaConCotizacionesEquifax][" + rut
                        + "] antes de realizar la consulta");
            }
            rentaSolicitante = antecedentesPrevisionalesDAO.obtenerInformeCotizacionesPrevisionalesEquifax(
                    canal, rut, dv, idAgenteAtencion, afp, folioValidacion, fechaCertificado);
            if (getLogger().isEnabledFor(Level.DEBUG)) {
                getLogger().debug("[obtenerRentaConCotizacionesEquifax][" + rut + "] rentaSolicitante: "
                        + rentaSolicitante);
            }
            boolean isClienteVigente = true;
            if (rentaSolicitante != null) {
                try {
                    ServiciosCliente servicioCliente = obtenerInstanciaSeriviciosCliente();
                    ClientePersona clientePersona = servicioCliente.getClientePersona(rut, dv);
                    if (getLogger().isEnabledFor(Level.DEBUG)) {
                        getLogger().debug("[obtenerRentaConCotizacionesEquifax][" + rut + "] isClienteVigente: "
                                + isClienteVigente);
                    }
                    if (getLogger().isEnabledFor(Level.DEBUG)) {
                        getLogger().debug("[obtenerRentaConCotizacionesEquifax][" + rut + "] getRentaFija: "
                                + clientePersona.getRentaFija());
                        getLogger().debug("[obtenerRentaConCotizacionesEquifax][" + rut + "] getRentaVariable: "
                                + clientePersona.getRentaVariable());
                    }
                    rentaSolicitante.setRentaFija(clientePersona.getRentaFija());
                    rentaSolicitante.setRentaVariable(clientePersona.getRentaVariable());
                    if (getLogger().isEnabledFor(Level.DEBUG)) {
                        getLogger().debug("[obtenerRentaConCotizacionesEquifax][" + rut + "] rentaSolicitante: "
                                + rentaSolicitante);
                    }
                }
                catch (Exception e){
                    if (getLogger().isEnabledFor(Level.ERROR)) {
                        getLogger().error("[obtenerRentaConCotizacionesEquifax][" + rut + "][Exception] ", e);
                    }
                    isClienteVigente = false;
                }
                rentaSolicitante.setFechaCertificado(fechaCertificado);
                ReglaRentaTO reglaRenta = evaluarReglasEquifax(rentaSolicitante, isClienteVigente);

                if (getLogger().isEnabledFor(Level.DEBUG)) {
                    getLogger().debug("[obtenerRentaConCotizacionesEquifax][" + rut + "] reglaRenta: "
                            + reglaRenta);
                }
                CotizacionClienteVO cotizacionCliente = rentaSolicitante.getCotizacionClienteVO();
                if (!reglaRenta.isCumpleRegla()) {
                    cotizacionCliente.setEstadoRenta(TablaValores.getValor(ARCHIVO_PARAMETROS,
                            "EST_RENTA_NOCALCULADA", "valor"));
                    cotizacionCliente.setMontoRenta(0);
                    if (getLogger().isEnabledFor(Level.DEBUG)) {
                        getLogger().debug("[obtenerRentaConCotizacionesEquifax][" + rut + "] monto seteado en 0");
                        getLogger().debug("[obtenerRentaConCotizacionesEquifax][" + rut + "] glosa error regla:"
                                + reglaRenta.getGlosa());
                    }
                    ErrorCotizacionVO errorCotizacion = new ErrorCotizacionVO();
                    rentaSolicitante.setEstadoErrorRentas(true);
                    errorCotizacion.setCodigoError(String.valueOf(rentaSolicitante.getCodigoRetorno()));
                    errorCotizacion.setDetalleError(reglaRenta.getGlosa());
                    rentaSolicitante.setErrorCotizacionVO(errorCotizacion);
                }
                else {

                    double variabilidad = 0;
                    String factorVariabilidad = null;

                    String topeUf = TablaValores.getValor(TABLA_PARAMETROS, "MONTO_MAX_CAL_EQUIFAX", "UF");
                    if (getLogger().isEnabledFor(Level.DEBUG)) {
                        getLogger().debug("[obtenerRentaConCotizacionesEquifax][" + rut + "] topeUf:" + topeUf);
                    }

                    Date fechaHoy = new Date();
                    if (getLogger().isEnabledFor(Level.DEBUG)) {
                        getLogger().debug("[obtenerRentaConCotizacionesEquifax][" + rut + "] fechaHoy:"
                                + fechaHoy);
                    }

                    double valorUf = WCorpUtils.getValorUF(fechaHoy);
                    if (getLogger().isEnabledFor(Level.DEBUG)) {
                        getLogger().debug("[obtenerRentaConCotizacionesEquifax][" + rut + "] valorUf:" + valorUf);
                    }


                    double topePeso = Double.parseDouble(topeUf) * valorUf;
                    if (getLogger().isEnabledFor(Level.DEBUG)) {
                        getLogger().debug("[obtenerRentaConCotizacionesEquifax][" + rut + "] topePeso:" 
                                + topePeso);
                    }
                    double ultimaRenta = 0;
                    
                    String[] periodo = ReglaRentaUtil.periodoMesesCotizados(1, cotizacionCliente
                            .getDetalleCotizacion()[0]);
                    
                    if (getLogger().isEnabledFor(Level.DEBUG)) {
                        getLogger().debug("[obtenerRentaConCotizacionesEquifax][" + rut + "] último período : "
                                + periodo);
                    }
                    
                    DetalleCotizacionClienteVO[] cotizacionesTemp = ReglaRentaUtil.cotizacionesEnPeriodo(
                            cotizacionCliente.getDetalleCotizacion(), periodo);
                    if (getLogger().isEnabledFor(Level.DEBUG)) {
                        getLogger().debug("[obtenerRentaConCotizacionesEquifax]["+rut+"] cotizaciones último período : "
                                + StringUtil.contenidoDe(cotizacionesTemp));
                    }
                    if (cotizacionesTemp!=null && cotizacionesTemp.length >0){
                        for (int i = 0; i<cotizacionesTemp.length; i++){
                            ultimaRenta = ultimaRenta + cotizacionesTemp[i].getRemuneracionImponible();
                        }
                    }

                    if (getLogger().isEnabledFor(Level.DEBUG)) {
                        getLogger().debug("[obtenerRentaConCotizacionesEquifax][" + rut + "] ultimaRenta:"
                                + ultimaRenta);
                    }

                    if (ultimaRenta < topePeso) {
                        variabilidad = SolicitudInstantaneaUtil.obtenerVariabilidadRentasEquifax(rentaSolicitante
                                .getCotizacionClienteVO().getDetalleCotizacion());
                        if (getLogger().isEnabledFor(Level.DEBUG)) {
                            getLogger().debug("[obtenerRentaConCotizacionesEquifax][" + rut + "] variabilidad: "
                                    + variabilidad);
                        }

                        factorVariabilidad = TablaValores.getValor(TABLA_PARAMETROS, "VAL_EQUIFAX",
                                "comparadorVariabilidad");
                        if (getLogger().isEnabledFor(Level.DEBUG)) {
                            getLogger().debug("[[obtenerRentaConCotizacionesEquifax][" + rut
                                    + "] factorVariabilidad: " + factorVariabilidad);
                        }

                        if (variabilidad > Double.parseDouble(factorVariabilidad)) {
                            if (getLogger().isEnabledFor(Level.DEBUG)) {
                                getLogger().debug("[[obtenerRentaConCotizacionesEquifax][" + rut
                                        + "] Se asignara renta por variabilidad");
                            }
                            cotizacionCliente = SolicitudInstantaneaUtil
                                    .asignarRentaSegunVariabilidad(variabilidad, cotizacionCliente
                                            , reglaRenta.getRentaCalculada());
                        } 
                        else {
                            if (getLogger().isEnabledFor(Level.DEBUG)) {
                                getLogger().debug("[[obtenerRentaConCotizacionesEquifax][" + rut
                                        + "] Se asignara renta fija: " + reglaRenta.getRentaCalculada());
                            }
                            cotizacionCliente.setMontoRentaFija(reglaRenta.getRentaCalculada());
                        }
                    }
                    else{
                        ErrorCotizacionVO errorCotizacion = new ErrorCotizacionVO();
                        rentaSolicitante = new RentaSolicitanteTO();
                        rentaSolicitante.setEstadoErrorRentas(true);
                        String glosa = TablaValores.getValor(SolicitudInstantaneaUtil.TABLA_SOLICITUD_INSTANTANEA,
                                "MSG_RENTA_EQUIFAX_03", "Desc");
                        errorCotizacion.setDetalleError(glosa);
                        rentaSolicitante.setErrorCotizacionVO(errorCotizacion);
                    }
                }
                if (getLogger().isEnabledFor(Level.DEBUG)) {
                    getLogger().debug("[obtenerRentaConCotizacionesEquifax][" + rut + "] rentaSolicitante: "
                            + rentaSolicitante);
                }

            }
            else{
                ErrorCotizacionVO errorCotizacion = new ErrorCotizacionVO();
                rentaSolicitante = new RentaSolicitanteTO();
                rentaSolicitante.setEstadoErrorRentas(true);
                String glosa = TablaValores.getValor(SolicitudInstantaneaUtil.TABLA_SOLICITUD_INSTANTANEA,
                        "CODIGO_RETORNO_WS_SINACOFI", String.valueOf(rentaSolicitante.getCodigoRetorno()));
                errorCotizacion.setCodigoError(String.valueOf(rentaSolicitante.getCodigoRetorno()));
                errorCotizacion.setDetalleError(glosa);
                rentaSolicitante.setErrorCotizacionVO(errorCotizacion);
                if (getLogger().isEnabledFor(Level.INFO)) {
                    getLogger().info("[obtenerRentaConCotizacionesEquifax][" + rut+"][BCI_FINOK]");
                }
            }
            return rentaSolicitante;

        }
        catch (GeneralException ge){
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[obtenerRentaConCotizacionesEquifax][" + rut
                        +"][BCI_FINEX][GeneralException] " +ge.getMessage(), ge);
            }
            ErrorCotizacionVO errorCotizacion = new ErrorCotizacionVO();
            RentaSolicitanteTO rentaSolicitante = new RentaSolicitanteTO();
            rentaSolicitante.setEstadoErrorRentas(true);
            errorCotizacion.setDetalleError(ge.getMessage());
            rentaSolicitante.setErrorCotizacionVO(errorCotizacion);
            return rentaSolicitante;
        }
        catch (Exception e) {
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[obtenerRentaConCotizacionesEquifax][" + rut +"][BCI_FINEX][Exception] "
                        +e.getMessage(), e);
            }
            throw new GeneralException("ESPECIAL",e.toString());
        }

    }

/**
     * Valida la renta para el canal Nova.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 25/08/2014 Manuel Escárate (Bee S.A.): Version inicial.
     * </ul>
     * @param reglaRenta renta calculada de previred.     
     * @return boolean true o false.                                          
     * @throws Exception 
     * @since 1.0
     */
    public boolean validaRentaNova(ReglaRentaTO reglaRenta)
            throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("[CalculoRentaBean.validaRentaNova] En validaRentaNova [BCI_INI]");
        }
        boolean actualizaRentaNova = false;
        int montoMinimoNOVA = Integer.valueOf(TablaValores.getValor(
                "CalculoRenta.parametros", "montoMinimoNova", "monto")).intValue();
        
        double monto = reglaRenta.getRentaCalculada();
        if (montoMinimoNOVA < monto){ 
            actualizaRentaNova = true;
        }
        if (log.isDebugEnabled()) {
            log.debug("[CalculoRentaBean.validaRentaNova] En validaRentaNova [BCI_FIN]");
        }
        return actualizaRentaNova;
    }
    
    
    /**
     * Inserta registro en la RTA.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 25/08/2014 Manuel Escárate (Bee S.A.): Version inicial.
     * </ul>
     * @param cotizacionCliente renta del cliente.                                                  
     * @return int 0 o 1.
     * @throws Exception 
     * @since 1.0
     */
    public int ingresarRegistroRTA(CotizacionClienteVO cotizacionCliente) throws Exception {
        int resultado = 0;
        if (log.isDebugEnabled()) {log.debug("[CalculoRentaBean.ingresarRegistroRTA] En ingresarRegistroRTA");}
        try {
            CalculoRentaDAO calculoRentaDAO = new CalculoRentaDAO();
            if (log.isDebugEnabled()) {
                      log.debug("[CalculoRentaBean.ingresarRegistroRTA]"
                    + " Luego de instanciar el DAO[" + calculoRentaDAO + "]"); 
                 }
            resultado = calculoRentaDAO.ingresarRegistroRTA(cotizacionCliente);
            if (log.isDebugEnabled()) {
                    log.debug("[CalculoRentaBean.ingresarRegistroRTA]"
                    + " resultado ["+resultado+"]");
                 }
        } 
        catch (Exception ex) {
            if( log.isEnabledFor(Level.ERROR) ){
                log.error("[CalculoRentaBean.ingresarRegistroRTA]:Error [" + ex.toString() + "]");
            }
            throw ex;
        }
        return resultado;
    }
    
    /**
     * retorna una instancia del EJB Cliente.
     * <P>
     * Registro de versiones:
     * <UL>
     * <LI> 1.0 25/08/2014 Manuel Escárate R. (BEE): Versión inicial.</li>
     * </UL>
     * <P>
     *
     * @return Cliente instancia del ejb Cliente.
     * @throws Exception en caso de error.
     * @since 1.0
     */
    private Cliente getClienteBean() throws Exception {
        Cliente clienteBean = null;
        try {
            ClienteHome homeCliente = (ClienteHome) EnhancedServiceLocator
                    .getInstance().getHome("wcorp.bprocess.cliente.Cliente",ClienteHome.class);
            clienteBean = homeCliente.create();
        }
        catch (Exception ex) {
            if(log.isEnabledFor(Level.ERROR)){
                log.error("[getClienteBean] Exception = " + ErroresUtil.extraeStackTrace(ex));
            }
            throw ex;
        }
        return clienteBean;
    }    
    
    /**
    * Método que retorna una instancia del ejb Antecedentes Cliente.
    * <p>
    * Registro de versiones:
    * <ul>
    * <li>1.0 08/07/2016 Hernán Rodriguez (TInet) - Claudia López (Ing. Soft. BCI): versión inicial.
    * </ul>
    * <p>
    * 
    * @return AntecedentesClientes
    * @throws Exception en caso de haber algun error.
    * @since 11.5
    */
    private AntecedentesClientes getEjbAntecedentesCliente() throws Exception{
        if (this.antecedentesClientes == null) {
            AntecedentesClientesHome home = (AntecedentesClientesHome) EnhancedServiceLocator.getInstance()
                .getHome(JNDI_ANTECEDENTES_CLIENTE, AntecedentesClientesHome.class);
            this.antecedentesClientes = home.create();
        }
        return this.antecedentesClientes;
    }
    
     /**
     * Método que graba encabezado del cálculo automático.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 19/03/2015 Alejandro Barra (SEnTRA) - Andrés Alvarado (Ing. Soft. BCI): versión inicial.
     * </ul>
     * <p>
     * 
     * @param encabezadoCalculoAutomatico EncabezadoCalculoAutomaticoTO
     * @return ResultadoRegistroRentaTO
     * @throws RemoteException en caso de haber algun error en la llamada al DAO. 
     * @throws Exception en caso de haber algun error.
     * @since 11.0
     */
    public ResultadoRegistroRentaTO grabaEncabezadoCalculoAutomatico(EncabezadoCalculoAutomaticoTO 
                    encabezadoCalculoAutomatico) throws RemoteException, Exception{
        if(getLogger().isEnabledFor(Level.INFO)){
            getLogger().info("[grabaEncabezadoCalculoAutomatico][BCI_INI]");
        }
        ResultadoRegistroRentaTO resultadoRegistroRentaTO = new ResultadoRegistroRentaTO();
        try{
        	
            CalculoRentaDAO calculoRentaDAO = new CalculoRentaDAO();
            resultadoRegistroRentaTO = calculoRentaDAO
                     .grabaEncabezadoCalculoAutomatico(encabezadoCalculoAutomatico);
        }
        catch(RemoteException re){
            if(getLogger().isEnabledFor(Level.ERROR)){
                getLogger().error("[grabaEncabezadoCalculoAutomatico][BCI_FINEX][RemoteException] error"
                + " con mensaje: " + re.getMessage(), re);
            }
            throw re;
        }
        catch(Exception e){
            if(getLogger().isEnabledFor(Level.ERROR)){
                getLogger().error("[grabaEncabezadoCalculoAutomatico][BCI_FINEX][Exception] error"
                + " con mensaje: " + e.getMessage(), e);
            }
            throw e;
        }
        if(getLogger().isEnabledFor(Level.INFO)){
            getLogger().info("[grabaEncabezadoCalculoAutomatico][BCI_FINOK] retornando resultadoRegistroRentaTO "
                + resultadoRegistroRentaTO);
        }
        return resultadoRegistroRentaTO;
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
     * @param detalleCotizacionCliente DetalleCotizacionClienteVO[]
     * @param encabezadoCalculoAutomatico EncabezadoCalculoAutomaticoTO
     * @return resultadoDetallesCalculoAutomatico ResultadoRegistroRentaTO
     * @throws Exception en caso de haber algun error.
     * @since 11.0
     */
    public ResultadoRegistroRentaTO grabaDetallesCalculoAutomatico(
           DetalleCotizacionClienteVO[] detalleCotizacionCliente, 
           EncabezadoCalculoAutomaticoTO encabezadoCalculoAutomatico) throws Exception {
        
        if(getLogger().isEnabledFor(Level.INFO)){
            getLogger().info("[grabaDetallesCalculoAutomatico][BCI_INI]");
        }
        ResultadoRegistroRentaTO resultadoDetallesCalculoAutomatico = new ResultadoRegistroRentaTO();
        
        try{
            CalculoRentaDAO calculoRentaDAO = new CalculoRentaDAO();
        
            if(detalleCotizacionCliente != null && detalleCotizacionCliente.length > 0){
                for(int i = 0 ; i < detalleCotizacionCliente.length ; i++){
                    resultadoDetallesCalculoAutomatico = calculoRentaDAO
                                    .grabaDetallesCalculoAutomatico(detalleCotizacionCliente[i]
                                    , encabezadoCalculoAutomatico);
                }
            }
        }
        catch(Exception e){
            if(getLogger().isEnabledFor(Level.ERROR)){
                getLogger().error("[grabaDetallesCalculoAutomatico][BCI_FINEX][Exception] error"
                + " con mensaje: " + e.getMessage(), e);
            }
            throw e;
        }
        
        if(getLogger().isEnabledFor(Level.INFO)){
            getLogger().info("[grabaDetallesCalculoAutomatico][BCI_FINOK] retornando "
                + "resultadoDetallesCalculoAutomatico " + resultadoDetallesCalculoAutomatico);
        }
        return resultadoDetallesCalculoAutomatico;
    }
    
     /**
     * Método que consulta el encabezado del cálculo automático.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 19/03/2015 Alejandro Barra (SEnTRA) - Andrés Alvarado (Ing. Soft. BCI): versión inicial.
     * </ul>
     * <p>
     * 
     * @param secuencia int
     * @return encabezado EncabezadoCalculoAutomaticoTO
     * @throws RemoteException en caso de haber algun error en la llamada al DAO. 
     * @throws Exception en caso de haber algun error.
     * @since 11.0
     */
    public EncabezadoCalculoAutomaticoTO consultaEncabezadoCalculoAutomatico(int secuencia)
        throws RemoteException, Exception {
        if(getLogger().isEnabledFor(Level.INFO)){
            getLogger().info("[consultaEncabezadoCalculoAutomatico][BCI_INI] inicio secuencia[" + secuencia + "]");
        }
        EncabezadoCalculoAutomaticoTO encabezadoCalculoAutomaticoTO  = new EncabezadoCalculoAutomaticoTO();
        try{
            CalculoRentaDAO calculoRentaDAO = new CalculoRentaDAO();
            encabezadoCalculoAutomaticoTO =  calculoRentaDAO.consultaEncabezadoCalculoAutomatico(secuencia);
        }
        catch(RemoteException re){
            if(getLogger().isEnabledFor(Level.ERROR)){
                getLogger().error("[consultaEncabezadoCalculoAutomatico][BCI_FINEX][RemoteException] error"
                + " con mensaje: " + re.getMessage(), re);
            }
            throw re;
        }
        catch(Exception e){
            if(getLogger().isEnabledFor(Level.ERROR)){
                getLogger().error("[consultaEncabezadoCalculoAutomatico][BCI_FINEX][Exception] error"
                + " con mensaje: " + e.getMessage(), e);
            }
            throw e;
        }
        
        if(getLogger().isEnabledFor(Level.INFO)){
            getLogger().info("[consultaEncabezadoCalculoAutomatico][BCI_FINOK] retornando "
                + "encabezadoCalculoAutomaticoTO " + encabezadoCalculoAutomaticoTO);
        }
        return encabezadoCalculoAutomaticoTO;
    }
    
     /**
     * Método que consulta el detalle del cálculo automático.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 19/03/2015 Alejandro Barra (SEnTRA) - Andrés Alvarado (Ing. Soft. BCI): versión inicial.
     * </ul>
     * <p>
     * 
     * @param secuencia int
     * @return detallecotizacion DetalleCotizacionClienteVO[]
     * @throws RemoteException en caso de haber algun error en la llamada al DAO. 
     * @throws Exception en caso de haber algun error.
     * @since 11.0
     */
    public DetalleCotizacionClienteVO[] consultaDetallesCalculoAutomatico(int secuencia)
           throws RemoteException, Exception {

        if(getLogger().isEnabledFor(Level.INFO)){
            getLogger().info("[consultaDetallesCalculoAutomatico][BCI_INI] inicio secuencia[" + secuencia + "]");
        }
        DetalleCotizacionClienteVO[] detalleCotizacionClienteVO = null;
        try{
            CalculoRentaDAO calculoRentaDAO = new CalculoRentaDAO();
            detalleCotizacionClienteVO = calculoRentaDAO.consultaDetallesCalculoAutomatico(secuencia);
        }
        catch(RemoteException re){
            if(getLogger().isEnabledFor(Level.ERROR)){
                getLogger().error("[consultaDetallesCalculoAutomatico][BCI_FINEX][RemoteException] error"
                + " con mensaje: " + re.getMessage(), re);
            }
            throw re;
        }
        catch(Exception e){
            if(getLogger().isEnabledFor(Level.ERROR)){
                getLogger().error("[consultaDetallesCalculoAutomatico][BCI_FINEX][Exception] error"
                + " con mensaje: " + e.getMessage(), e);
            }
            throw e;
        }
        
        if(getLogger().isEnabledFor(Level.INFO)){
            getLogger().info("[consultaDetallesCalculoAutomatico][BCI_FINOK]");
        }
        return detalleCotizacionClienteVO;
    }
    
    /**
     * Método que actualiza la renta de un cliente luego de la aplicación de reglas definidas para estos efectos.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 15/03/2016 Andrés Cea S. (TINet) - Oliver Hidalgo (Ing. Soft. BCI): Versión inicial.</li>
     * <li>1.1 24/05/2016 Rafael Pizarro (TINet) - Hernán Rodriguez (TINet) - Claudia López (Ing. Soft. BCI): Se cambia la logica que 
     * determina cuando ir a previred, basandose en la información del cliente obtenida desde SGC. se modifica retorno para poder
     * obtener información relacionada con la ejecución del servicio.</li>
     * </ul>
     * </p>
     * @param rut rut del prospecto.
     * @param dv dv del prospecto.
     * @param canal canal de la consulta.
     * @param codServicio codigo servicio.
     * @return String con codigo de respuesta de ejecución.
     * @throws GeneralException Excepcion de negocio.
     * @since 11.5
     */
    public String actualizarRentaClienteCotizaciones(long rut, char dv, String canal, String codServicio)
        throws GeneralException {
        getLogger().info("[actualizarRentaClienteCotizaciones] [BCI_INI]");
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("[actualizarRentaClienteCotizaciones] [" + rut + "] rut= " + rut);
            getLogger().debug("[actualizarRentaClienteCotizaciones] [" + rut + "] dv= " + dv);
            getLogger().debug("[actualizarRentaClienteCotizaciones] [" + rut + "] canal= " + canal);
            getLogger().debug("[actualizarRentaClienteCotizaciones] [" + rut + "] codServicio= " + codServicio);
        }
        CotizacionClienteVO cotizacionCliente = new CotizacionClienteVO();
        RentaTO renta = null;
        int codigoRegistro = -1;
        CotizacionTO[] detalleCotizacionesPrevired = null;
        CotizacionPreviredTO cotizacionPreviredTO = null;
        boolean rentaActualizada = false;
        ServiciosCliente srvCliente;
        try {
            srvCliente = obtenerInstanciaSeriviciosCliente();
        }
        catch (Exception e1) {
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[actualizarRentaClienteCotizaciones][BCI_FINEX][Exception] error"
                    + " con mensaje: " + e1.getMessage(), e1);
            }
            throw new GeneralException(ERROR_GENERICO_0002);
        }
        RetornoTipCli retornoTipCli = null;
        String codMov = TablaValores.getValor(TABLA_PARAMETROS_CALCULORENTA, "RentaEstimadaPrevired", "valor");
        String codUsuarioAct = TablaValores.getValor(TABLA_PARAMETROS_CALCULORENTA, "usuarioActPrevired", "valor");
        try {
            retornoTipCli = srvCliente.consultaAntecedentes(rut, dv);
        }
        catch (Exception e) {
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[actualizarRentaClienteCotizaciones][BCI_FINEX][Exception] error"
                    + " con mensaje: " + e.getMessage(), e);
            }
            throw new GeneralException(ERROR_GENERICO_0025);
        }

        if (retornoTipCli != null) {
            if (retornoTipCli.TipoCliente.equalsIgnoreCase(TIPO_CLIENTE_PERSONA_SGC)) {
                DatosPersona datosPersona = retornoTipCli.getDatosPersona();
                if (datosPersona != null) {
                    if (getLogger().isEnabledFor(Level.DEBUG)) {
                        getLogger().debug("[actualizarRentaClienteCotizaciones][" + datosPersona
                            + "] datosPersona: " + datosPersona);
                    }
                    ResultConsultaAntecedentesEconomicosPersonasTO antecedentesEconomicosPersonas = null;
                    Date fechaUltimaModificacion = null;
        try {
                        antecedentesEconomicosPersonas = getEjbAntecedentesCliente().consultaAntecedentesEconomicosPersonas(new MultiEnvironment(), null, Integer.valueOf(datosPersona.getRut()).intValue(), datosPersona.getDigitoVerificador().charAt(0), datosPersona.getIndActualiza(),null, null);
                        fechaUltimaModificacion = antecedentesEconomicosPersonas.getFechaUltimaActualizacion();
                        Date fechaActual = new Date();
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(fechaActual);
                        if (getLogger().isDebugEnabled()) {
                            getLogger().debug("[actualizarRentaClienteCotizaciones] [" + rut + "] fechaActual=<"
                                + FechasUtil.convierteDateAString(fechaActual, "dd/MM/yyyy") + ">");
                        }
                        cal.add(Calendar.MONTH, MESES_ANTIGUEDAD_SGC);
                        if (getLogger().isDebugEnabled()) {
                            getLogger().debug("[actualizarRentaClienteCotizaciones] [" + rut + "] fechaMenos3Meses=<"
                                + FechasUtil.convierteCalendarAString(cal, "dd/MM/yyyy") + ">");
                        }
                        Calendar cal2 = Calendar.getInstance();
                        cal2.setTime(fechaUltimaModificacion);
                        if (FechasUtil.comparaDias(cal2, cal) != 1) {
                            if (getLogger().isDebugEnabled()) {
                            getLogger().debug("[actualizarRentaClienteCotizaciones] [" + rut
                                + "] [Pasaron 3 meses, se debe ir a previred]");
                            }
                            cotizacionCliente.setObtenidoDesdePrevired(true);
                        }
                    }
                    catch (ClientesException ce) {
                        if (getLogger().isEnabledFor(Level.ERROR)) {
                            getLogger().error("[actualizarRentaClienteCotizaciones][BCI_FINEX][ClientesException] error"
                                + " con mensaje: " + ce.getMessage(), ce);
                        }
                        if ( ce.getInfoAdic().indexOf(CLIENTE_SIN_ANTECEDENTES_ECONOMICOS) > 0 ) {
                            if (getLogger().isEnabledFor(Level.DEBUG)) {
                                getLogger().error("[actualizarRentaClienteCotizaciones][BCI_FINEX][ClientesException] "
                                    + CLIENTE_SIN_ANTECEDENTES_ECONOMICOS + " Se debe actualizar con previred.");
                            }
                            cotizacionCliente.setObtenidoDesdePrevired(true);
                        } else {
                            if (getLogger().isEnabledFor(Level.ERROR)) {
                                getLogger().error("[actualizarRentaClienteCotizaciones][BCI_FINEX][ClientesException] error"
                                    + " con mensaje: " + ce.getMessage(), ce);
                            }
                            throw new GeneralException(ERROR_GENERICO_0002);
            }
        }
        catch (Exception e) {
            if (getLogger().isEnabledFor(Level.ERROR)) {
                            getLogger().error("[actualizarRentaClienteCotizaciones][BCI_FINEX][Exception] error"
                                + " con mensaje: " + e.getMessage(), e);
            }
                        throw new GeneralException(ERROR_GENERICO_0002);
                    }                    
                    if (getLogger().isEnabledFor(Level.DEBUG)) {
                        getLogger().debug("[actualizarRentaClienteCotizaciones] fechaUltimaModificacion: ["
                            + FechasUtil.convierteDateAString(fechaUltimaModificacion, "dd/MM/yyyy")
                            + "] antecedentesEconomicosPersonas: [" + antecedentesEconomicosPersonas + "]");
                    }
                }
                else {
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("[actualizarRentaClienteCotizaciones] [" + rut
                            + "] [No vienen datos de persona desde " + "SGC, se va a previred.]");
                    }
                    cotizacionCliente.setObtenidoDesdePrevired(true);
                }
            }
            else {
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug(
                        "[actualizarRentaClienteCotizaciones] [" + rut + "] [BCI_FINEX] [Cliente no es PERSONA]");
                }
                throw new GeneralException(ERROR_GENERICO_0025);
            }
        }
        else {
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("[actualizarRentaClienteCotizaciones] [" + rut
                    + "] [BCI_FINEX] [NO existen datos en SGC para el cliente.]");
            }
            throw new GeneralException(ERROR_GENERICO_0073);
        }

        if (cotizacionCliente.esObtenidoDesdePrevired()) {
            if (getLogger().isDebugEnabled()) {
                getLogger().debug(
                    "[actualizarRentaClienteCotizaciones] [" + rut + "] cotización se obtiene desde previred.");
                getLogger().debug("[actualizarRentaClienteCotizaciones] [" + rut
                    + "] Se cargan datos encabezado y se mapea a rentaTO.");
            }
            try {
            cotizacionCliente = cargarDatosEncabezadoCotizacion(canal, rut, dv, codServicio);
            renta = mapeaCotizacionClienteVOARentaTO(cotizacionCliente);
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("[actualizarRentaClienteCotizaciones] [" + rut + "] rentaTO=" + renta);
            }
                codigoRegistro = ingresarEncabezadoCotizacion(cotizacionCliente);
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug(
                        "[actualizarRentaClienteCotizaciones] [" + rut + "] codigoRegistro=" + codigoRegistro);
                }
                cotizacionCliente.setIdentificador(codigoRegistro);
                renta.setIdentificador(codigoRegistro);
            }
            catch (Exception e) {
                if (getLogger().isEnabledFor(Level.ERROR)) {
                    getLogger().error("[actualizarRentaClienteCotizaciones] [" + rut
                        + "] [Exception] [BCI_FINEX] return false. " + e.getMessage(), e);
                }
                return RESPUESTA_ACT_RENTA_0002;
            }

            cotizacionPreviredTO = obtenerDetalleCotizacionesPrevired(rut, dv, canal, codigoRegistro, codServicio);
            detalleCotizacionesPrevired = ((cotizacionPreviredTO != null) ? cotizacionPreviredTO.getCotizaciones()
                : null);
            if (detalleCotizacionesPrevired == null) {
                if (getLogger().isEnabledFor(Level.ERROR)) {
                    getLogger().error("[actualizarRentaClienteCotizaciones] [" + rut + "] "
                        + "No obtuvo cotizaciones desde previred.");
                    getLogger().error("[actualizarRentaClienteCotizaciones] [" + rut + "] "
                        + "detalleCotizacionesPrevired= " + StringUtil.contenidoDe(detalleCotizacionesPrevired));
                }
                return RESPUESTA_ACT_RENTA_0003;
            }

            renta.setCotizaciones(detalleCotizacionesPrevired);
            renta.setOrigen("Previred");
                
            cotizacionCliente = mapeaRentaTOACotizacionClienteVO(renta);
                }
        else {
            if (getLogger().isEnabledFor(Level.DEBUG)) {
                    getLogger().debug("[actualizarRentaClienteCotizaciones] [" + rut
                    + "] [BCI_FINOK] Renta actualizada hace menos de 3 meses en SGC, return true. ");
                }
            return RESPUESTA_ACT_RENTA_0001;
                }

            try {
                renta = EvaluadorActualizacionRenta.evaluar(new CalculoRentaClienteStrategy(renta, canal));
                
                if (renta == null) {
                    if (getLogger().isEnabledFor(Level.ERROR)) {
                        getLogger()
                            .error("[actualizarRentaClienteCotizaciones] [" + rut + "] [BCI_FINEX] renta es nula");
                    }
                boolean estadoRegistro = registrarInformacionAdicionalError(
                    new Exception(
                        TablaValores.getValor(TABLA_PARAMETROS_CALCULORENTA, RESPUESTA_ACT_RENTA_0004, "Desc")),
                    canal, rut, dv, codigoRegistro,
                    TablaValores.getValor(ARCHIVO_PARAMETROS, "EST_RENTA_NOCALCULADA", "valor"), codServicio);
                if (getLogger().isEnabledFor(Level.ERROR)) {
                    getLogger().warn("[actualizarRentaClienteCotizaciones] [" + rut
                        + "] [Exception] estadoRegistro=" + estadoRegistro);
                }
                return RESPUESTA_ACT_RENTA_0004;
                }
                cotizacionCliente = mapeaRentaTOACotizacionClienteVO(renta);
            }
            catch (Exception e) {
                if (getLogger().isEnabledFor(Level.ERROR)) {
                    getLogger().error("[actualizarRentaClienteCotizaciones] [" + rut + "] "
                        + "No pasa las reglas de validación y calculo de renta. No se puede continuar.", e);
                }
            boolean estadoRegistro = registrarInformacionAdicionalError(e, canal, rut, dv, codigoRegistro,
                TablaValores.getValor(ARCHIVO_PARAMETROS, "EST_RENTA_NOCALCULADA", "valor"), codServicio);
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().warn("[actualizarRentaClienteCotizaciones] [" + rut + "] [Exception] estadoRegistro="
                    + estadoRegistro);
            }
            return RESPUESTA_ACT_RENTA_0004;
        }

        cotizacionCliente.setFolio(cotizacionPreviredTO.getFolio());
        cotizacionCliente.setFirma(cotizacionPreviredTO.getFirma());

        try {
            if (renta.isActualizarEnSGC()) {
                rentaActualizada = srvCliente.actualizaClientePersonaRenta(new Long(rut), String.valueOf(dv), "",
                    "", new Double(renta.getRentaVariable() / MILES), renta.getFechaActualizacion(),
                    new Double(renta.getRentaFija() / MILES), renta.getFechaActualizacion(), codMov,
                    codUsuarioAct);
                if (getLogger().isDebugEnabled()) {
                    getLogger()
                        .debug("[actualizarRentaClienteCotizaciones] [" + rut + "] sgc =" + rentaActualizada);
                }
            }
            cotizacionCliente
                .setEstadoRenta(TablaValores.getValor(ARCHIVO_PARAMETROS, "EST_RENTA_ACTUALIZADA", "valor"));
            renta.setEstado(TablaValores.getValor(ARCHIVO_PARAMETROS, "EST_RENTA_ACTUALIZADA", "valor"));
        }
        catch (Exception e) {
            if (getLogger().isEnabledFor(Level.WARN)) {
                getLogger().warn("[actualizarRentaClienteCotizaciones] [" + rut + "] [Exception] "
                    + "No se pudo actualizar la renta.", e);
            }
            boolean estadoRegistro = registrarInformacionAdicionalError(e, canal, rut, dv, codigoRegistro,
                TablaValores.getValor(ARCHIVO_PARAMETROS, "EST_RENTA_PROBLEMAACTUALIZACION", "valor"),
                codServicio);
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().warn("[actualizarRentaClienteCotizaciones] [" + rut + "] [Exception] estadoRegistro="
                    + estadoRegistro);
        }
            return RESPUESTA_ACT_RENTA_0005;
        }

        try {
            int ingresoCotizacion = ingresoCotizacionCliente(cotizacionCliente);
            if (getLogger().isDebugEnabled()) {
                getLogger().debug(
                    "[actualizarRentaClienteCotizaciones] [" + rut + "] ingresoCotizacion=" + ingresoCotizacion);
            }
        }
        catch (Exception e) {
            if (getLogger().isEnabledFor(Level.WARN)) {
                getLogger().warn("[actualizarRentaClienteCotizaciones] [" + rut + "] [Exception] "
                    + "No se pudo ingresar las cotizaciones en cotizacli.", e);
            }
            boolean estadoRegistro = registrarInformacionAdicionalError(e, canal, rut, dv, codigoRegistro,
                TablaValores.getValor(ARCHIVO_PARAMETROS, "EST_RENTA_PROBLEMAACTUALIZACION", "valor"),
                codServicio);
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().warn("[actualizarRentaClienteCotizaciones] [" + rut + "] [Exception] estadoRegistro="
                    + estadoRegistro);
            }
            return RESPUESTA_ACT_RENTA_0006;
        }

        try {
            if (renta.isActualizarEnDGC()) {
                int resultado = this.ingresaRentaPreviredDGCRTA(cotizacionCliente);
            if (getLogger().isDebugEnabled()) {
                    getLogger().debug(
                        "[actualizarRentaClienteCotizaciones] [" + rut + "] ingresarRegistroRTA=" + resultado);
                }
            }
        }
        catch (Exception e) {
            if (getLogger().isEnabledFor(Level.WARN)) {
                getLogger().warn("[actualizarRentaClienteCotizaciones] [" + rut + "] [Exception] "
                    + "No se pudo ingresar las cotizaciones en RTA. Esto no impide seguir.", e);
            }
            boolean estadoRegistro = registrarInformacionAdicionalError(e, canal, rut, dv, codigoRegistro,
                TablaValores.getValor(ARCHIVO_PARAMETROS, "EST_RENTA_PROBLEMAACTUALIZACION", "valor"),
                codServicio);
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().warn("[actualizarRentaClienteCotizaciones] [" + rut + "] [Exception] estadoRegistro="
                    + estadoRegistro);
            }
            return RESPUESTA_ACT_RENTA_0007;
        }

        try {
            int resultado = actualizaEstadoEncabezado(renta.getIdentificador(),
                cotizacionCliente.getEstadoRenta());
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("[actualizarRentaConCotizaciones] [" + rut + "] resultado=<" + resultado + ">");
            }
        }
        catch (Exception e) {
            if (getLogger().isEnabledFor(Level.WARN)) {
                getLogger().warn("[actualizarRentaClienteCotizaciones] [" + rut + "] [Exception] "
                    + "No se pudo actualizar el estado del encabezado de las cotizaciones.", e);
            }
        }

        return RESPUESTA_ACT_RENTA_0000;
    }
    
    /**
     * Método que registra informacion adicional de error.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 25/05/2016 Hernán Rodriguez (TINet) - Claudia Lopez (Ing. Soft. BCI): Versión inicial.</li>
     * </ul>
     * </p>
     * @param e excepcion que contiene detalle del error producido.
     * @param canal canal correspondiente a la ejecucion del servicio que actualiza renta.
     * @param rutCliente rut del cliente persona al cual se realiza la actualizacion de renta.
     * @param dvCliente digito verificador del rut cliente persona.
     * @param identificadoRegistro id de registro cotizacion.
     * @param estado estado actualizacion renta.
     * @param codServicio codigo de servicio que realiza la actualizacion de renta.
     * @return boolean resultado del registro de informacion adicional de error.
     * @since 11.5
     */
    private boolean registrarInformacionAdicionalError(Exception e, String canal, long rutCliente, char dvCliente,
        int identificadoRegistro, String estado, String codServicio) {
        boolean registro = false;
        try {
            CotizacionClienteVO cotCliente = cargarDatosCotizacionError(canal, rutCliente, dvCliente, identificadoRegistro,
                TablaValores.getValor(ARCHIVO_PARAMETROS, "EST_RENTA_PROBLEMAPREVIRED", "valor"),codServicio);

            ErrorCotizacionVO errorCotizacion = new ErrorCotizacionVO();
            String codError = TablaValores.getValor(ARCHIVO_PARAMETROS, "ERROR_PREVIRED", "errorGenerico");
            errorCotizacion.setCodigoError(codError);
            String motivoError = e.getMessage().length() > DET_ERROR_COT
                ? e.getMessage().substring(0, DET_ERROR_COT) : e.getMessage();
            errorCotizacion.setDetalleError(motivoError);
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("[registrarInformacionAdicionalError] [" + rutCliente + "] codError=" + codError
                    + ", motivoError=" + motivoError);
            }
            int respuesta = ingresaErrorCotizacion(cotCliente, errorCotizacion);
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("[registrarInformacionAdicionalError] [" + rutCliente + "] respuesta=" + respuesta);
            }
            if (respuesta > 0) {
                registro = true;
            }
        }
        catch (Exception ex) {
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[registrarInformacionAdicionalError] [" + rutCliente
                    + "] [Exception] [BCI_FINEX] retorna false.", e);
            }
        }
        return registro;
    }
    
    
    /**
     * Método que obtiene el detalle de las cotizaciones desde servicio de previred. Si la invocación al servicio se cae
     * registra el error de la consulta en la bdd cotizacli.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 15/03/2016 Andrés Cea S. (TINet) - Oliver Hidalgo (Ing. Soft. BCI): Versión inicial.</li>
     * <li>1.1 24/05/2016 Rafael Pizarro (TINet) - Claudia Lopez (Ing. Soft. BCI): Se modifica objeto de 
     * respuesta y se agrega logica para el manejo de errores enviados por el servicio.</li>
     * </ul>
     * </p>
     * @param rut rut del cliente a obtener cotizaciones.
     * @param dv digito verificador del cliente a obtener cotizaciones.
     * @param canal canal desde el cual se consulta.
     * @param codigoRegistro identificador del registro de encabezado de las cotizaciones.
     * @param codServicio corresponde al identificador de servicio utilizado.
     * @return CotizacionPreviredTO objeto de cotizaciones obtenidas desde previred.
     * @since 11.5
     */
    private CotizacionPreviredTO obtenerDetalleCotizacionesPrevired(long rut, char dv, String canal, int codigoRegistro, String codServicio) {
        CotizacionPreviredTO cotizacionPreviredTO = null;
        CotizacionTO[] detalleCotizacionesPrevired = null;
        try {
            if (getLogger().isInfoEnabled()) {
                getLogger().info("[actualizarRentaClienteCotizaciones] [" + rut + "] "
                    + "obteniendo cotizaciones desde servicio previred");
            }
            CalculoRentaDAO dao = new CalculoRentaDAO();
            cotizacionPreviredTO = dao.obtieneCotizacionesWSPrevired(rut, dv, codigoRegistro);
            if (cotizacionPreviredTO.getCodigo().equals(CODIGO_CONSULTA_EXITOSA_PREVIRED)){
                detalleCotizacionesPrevired = cotizacionPreviredTO.getCotizaciones();
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("[actualizarRentaClienteCotizaciones] [" + rut + "] "
                    + "detalleCotizacionesPrevired=" + StringUtil.contenidoDe(detalleCotizacionesPrevired));
            }
        }
            else{
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("[actualizarRentaClienteCotizaciones] [" + rut + "] "
                        + "[Error al obtener información desde previred]");
            }
                CotizacionClienteVO cotCliente = cargarDatosCotizacionError(canal, rut, dv, codigoRegistro,
                    TablaValores.getValor(ARCHIVO_PARAMETROS, "EST_RENTA_PROBLEMAPREVIRED", "valor"),codServicio);
                ErrorCotizacionVO errorCotizacion = new ErrorCotizacionVO();
                String codError = cotizacionPreviredTO.getCodigo();
                errorCotizacion.setCodigoError(codError);
                String motivoError = TablaValores.getValor(ARCHIVO_PARAMETROS, codError, "valor");
                errorCotizacion.setDetalleError(motivoError);
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("[actualizarRentaClienteCotizaciones] [" + rut + "] codError=" + codError
                        + ", motivoError=" + motivoError);
                }
                int respuesta = ingresaErrorCotizacion(cotCliente, errorCotizacion);
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("[actualizarRentaClienteCotizaciones] [" + rut + "] respuesta=" + respuesta);
                }
            }
                }
        catch (Exception e) {
            if (getLogger().isEnabledFor(Level.WARN)) {
                getLogger()
                    .warn("[actualizarRentaClienteCotizaciones] [" + rut + "] [Exception] " + e.getMessage(), e);
            }
            boolean estadoRegistro = registrarInformacionAdicionalError(e, canal, rut, dv,codigoRegistro, TablaValores.getValor(ARCHIVO_PARAMETROS, "EST_RENTA_PROBLEMAPREVIRED", "valor"), codServicio);
            if (getLogger().isEnabledFor(Level.WARN)) {
                getLogger()
                    .warn("[actualizarRentaClienteCotizaciones] [" + rut + "] [Exception] estadoRegistro=" + estadoRegistro);
            }
        }
        return cotizacionPreviredTO;
    }
    
    
    /**
     * Método que mapea un objeto {@code RentaTO} a un {@code CotizacionClienteVO}.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 15/03/2016 Andrés Cea S. (TINet) - Oliver Hidalgo (Ing. Soft. BCI): Versión inicial.</li>
     * <li>1.1 24/05/2016 Rafael Pizarro (TINet) - Claudia López (Ing. Soft. BCI): 
	 *         Se agrega seteo de parámetro EstadoError.</li>
     * </ul>
     * </p>
     * @param renta TO a mapear a un {@code CotizacionClienteVO}.
     * @return resultado del mapeo a un {@code CotizacionClienteVO}.
     * @since 11.5
     */
    private CotizacionClienteVO mapeaRentaTOACotizacionClienteVO(RentaTO renta) {
        CotizacionClienteVO cotizacionClienteVO = new CotizacionClienteVO();
        
        cotizacionClienteVO.setIdentificador(renta.getIdentificador());
        cotizacionClienteVO.setRutCliente(renta.getRut());
        cotizacionClienteVO.setDvCliente(renta.getDv());
        cotizacionClienteVO.setCanal(renta.getCanal());
        cotizacionClienteVO.setEstadoRenta(renta.getEstado());
        cotizacionClienteVO.setFechaConsulta(renta.getFechaActualizacion());
        cotizacionClienteVO.setMontoRentaFija(renta.getRentaFija());
        cotizacionClienteVO.setMontoRentaVariable(renta.getRentaVariable());
        cotizacionClienteVO.setServicio(renta.getServicio());
        cotizacionClienteVO.setEstadoError("");
        
        CotizacionTO[] cotizaciones = renta.getCotizaciones();
        List detalleCotizaciones = new ArrayList();
        if (cotizaciones != null) {
            for (int i = 0; i < cotizaciones.length; i++) {
                detalleCotizaciones.add(mapeaCotizacionTOADetalleCotizacionClienteVO(cotizaciones[i]));
            }
        }
        cotizacionClienteVO.setDetalleCotizacion(
                (DetalleCotizacionClienteVO[]) detalleCotizaciones.toArray(new DetalleCotizacionClienteVO[0]));
        
        return cotizacionClienteVO;
    }
    
    /**
     * Método que mapea un objeto {@code CotizacionClienteVO} a un {@code RentaTO}.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 15/03/2016 Andrés Cea S. (TINet) - Oliver Hidalgo (Ing. Soft. BCI): Versión inicial.</li>
     * </ul>
     * </p>
     * @param cotizacionCliente VO a mapear a un {@code RentaTO}.
     * @return resultado del mapeo a un {@code RentaTO}.
     * @since 11.3
     */
    private RentaTO mapeaCotizacionClienteVOARentaTO(CotizacionClienteVO cotizacionCliente) {
        RentaTO renta = new RentaTO();
        
        renta.setIdentificador(cotizacionCliente.getIdentificador());
        renta.setRut(cotizacionCliente.getRutCliente());
        renta.setDv(cotizacionCliente.getDvCliente());
        renta.setCanal(cotizacionCliente.getCanal());
        renta.setEstado(cotizacionCliente.getEstadoRenta());
        renta.setFechaActualizacion(cotizacionCliente.getFechaConsulta());
        renta.setRentaFija(cotizacionCliente.getMontoRentaFija());
        renta.setRentaVariable(cotizacionCliente.getMontoRentaVariable());
        renta.setServicio(cotizacionCliente.getServicio());
        
        DetalleCotizacionClienteVO[] detalleCotizaciones = cotizacionCliente.getDetalleCotizacion();
        List cotizaciones = new ArrayList();
        if (detalleCotizaciones != null) {
            for (int i = 0; i < detalleCotizaciones.length; i++) {
                cotizaciones.add(mapeaDetalleCotizacionClienteVOACotizacionTO(detalleCotizaciones[i]));
            }
        }
        renta.setCotizaciones((CotizacionTO[]) cotizaciones.toArray(new CotizacionTO[0]));
        
        return renta;
    }
    
    /**
     * Método que mapea un objeto {@code CotizacionTO} a un {@code DetalleCotizacionClienteVO}.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 15/03/2016 Andrés Cea S. (TINet) - Oliver Hidalgo (Ing. Soft. BCI): Versión inicial.</li>
     * <li>1.1 07/07/2016 Hernán Rodriguez (TINet) - Claudia López (Ing. Soft. BCI):
     *     Se Modifica asignacion de valores monto cotizacion y renta imponible.</li>
     * </ul>
     * </p>
     * @param cotizacion TO a mapear a un {@code DetalleCotizacionClienteVO}.
     * @return resultado del mapeo a un {@code DetalleCotizacionClienteVO}.
     * @since 11.5
     */
    private DetalleCotizacionClienteVO mapeaCotizacionTOADetalleCotizacionClienteVO(CotizacionTO cotizacion) {
        DetalleCotizacionClienteVO dc = new DetalleCotizacionClienteVO();
        
        dc.setRutEmpleador(cotizacion.getRutEmpleador());
        dc.setDvEmpleador(cotizacion.getDvEmpleador());
        dc.setPeriodo(FechasUtil.convierteDateAString(cotizacion.getMes(), FORMATO_FECHA_MMYYYY));
        dc.setMontoCotizacion(cotizacion.getMonto());
        dc.setRemuneracionImponible(cotizacion.getRemuneracionImponible());
        dc.setTipoMovimiento(cotizacion.getTipoMovimiento());
        dc.setFechaPago(cotizacion.getFechaPago());
        dc.setAfp(cotizacion.getAfp());
        dc.setFechaInformacionLegal(cotizacion.getFechaInformacionLegal());
        
        return dc;
    }
    
    /**
     * Método que mapea un objeto {@code DetalleCotizacionClienteVO} a un {@code CotizacionTO}.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 15/03/2016 Andrés Cea S. (TINet) - Oliver Hidalgo (Ing. Soft. BCI): Versión inicial.</li>
     * </ul>
     * </p>
     * @param detalleCotizacion VO a mapear a un {@code CotizacionTO}.
     * @return resultado del mapeo a un {@code CotizacionTO}.
     * @since 11.3
     */
    private CotizacionTO mapeaDetalleCotizacionClienteVOACotizacionTO(
        DetalleCotizacionClienteVO detalleCotizacion) {
        CotizacionTO cotizacion = new CotizacionTO();

        cotizacion.setRutEmpleador(detalleCotizacion.getRutEmpleador());
        cotizacion.setDvEmpleador(detalleCotizacion.getDvEmpleador());
        cotizacion.setMes(FechasUtil.convierteStringADate(detalleCotizacion.getPeriodo(),
            new SimpleDateFormat(FORMATO_FECHA_MMYYYY)));
        cotizacion.setRemuneracionImponible(detalleCotizacion.getMontoCotizacion());
        cotizacion.setTipoMovimiento(detalleCotizacion.getTipoMovimiento());
        cotizacion.setFechaPago(detalleCotizacion.getFechaPago());
        cotizacion.setAfp(detalleCotizacion.getAfp());
        cotizacion.setFechaInformacionLegal(detalleCotizacion.getFechaInformacionLegal());

        return cotizacion;
    }

    /**
     * Método que actualiza la renta de un prospecto en función de los datos obtenidos desde transfer o previred.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 15/03/2016 Ignacio González D. (TINet) - Oliver Hidalgo (Ing. Soft. BCI): Versión inicial.</li>
     * </ul>
     * </p>
     * @param rut rut del prospecto.
     * @param dv dv del prospecto.
     * @param canal canal de la consulta.
     * @param codServicio codigo servicio.
     * @param codigoViaje codigo del viaje del cliente.
     * @return true si la renta se actualiza correctamente, false en caso contrario.
     * @throws GeneralException En caso de exitir error mapeable según código.
     * @throws RemoteException Excepcion remota.
     * @throws Exception En caso de existir algún error.
     * @since 11.3
     */
    public boolean actualizarRentaProspectoViaje(long rut, char dv, String canal, String codServicio,
        int codigoViaje) throws GeneralException, RemoteException, Exception {
        if (getLogger().isEnabledFor(Level.INFO)) {
            getLogger().info("[actualizarRentaProspectoViaje][BCI_INI] Inicio.");
        }
        if (actualizarRentaProspectoTransfer(rut, dv, canal, codServicio, codigoViaje)) {
            if (getLogger().isEnabledFor(Level.INFO)) {
                getLogger().info("[actualizarRentaProspectoViaje][BCI_FINOK] Renta actualizada desde transfer.");
            }
            return true;
        }
        else {
            String consultaPrevired = TablaValores.getValor(TABLA_PARAMETROS_CALCULORENTA,
                "accesoConsultaPrevired_" + canal, "valor");
            if (consultaPrevired == null || consultaPrevired.equalsIgnoreCase("")) {
                consultaPrevired = "false";
            }
            boolean permisoAccesoPrevired = Boolean.valueOf(consultaPrevired).booleanValue();
            if (getLogger().isEnabledFor(Level.DEBUG)) {
                getLogger().debug("[actualizarRentaProspectoViaje] Permiso acceso a previred configurado desde ["
                    + TABLA_PARAMETROS_CALCULORENTA + "]: " + permisoAccesoPrevired);
            }
            if (permisoAccesoPrevired) {
                try {
                    if (!validarHorarioPrevired()) {
                        if (getLogger().isEnabledFor(Level.ERROR)) {
                            getLogger().error(
                                "[actualizarRentaProspectoViaje][GeneralException][BCI_FINEX]: Error previred, "
                                    + "fuera de horario establecido");
                        }
                        throw new GeneralException(ERROR_PREVIRED_FUERA_DE_HORARIO);
                    }
                }
                catch (Exception e) {
                    if (getLogger().isEnabledFor(Level.ERROR)) {
                        getLogger().error(
                            "[actualizarRentaProspectoViaje][GeneralException][BCI_FINEX]: Error previred, no se"
                                + " ha podido determinar horario de atención.");
                    }
                    throw new GeneralException(ERROR_VALIDA_SERVICIO_PREVIRED);
                }
                return actualizarRentaProspectoCotizaciones(rut, dv, canal, codServicio, codigoViaje);
            }
            else {
                if (getLogger().isEnabledFor(Level.ERROR)) {
                    getLogger().error(
                        "[actualizarRentaProspectoViaje][GeneralException][BCI_FINOK] Renta no ha sido posible"
                            + " actualizar desde transfer. y no tiene acceso a previred");
                }
                throw new GeneralException(ERROR_SIN_ACCESO_PREVIRED);
            }
        }
    }
    
    /**
     * Método que actualiza la renta de un prospecto en función de los datos obtenidos desde previred y luego de la 
     * aplicación de reglas definidas para estos efectos.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 15/03/2016 Andrés Cea S. (TINet) - Oliver Hidalgo (Ing. Soft. BCI): Versión inicial.</li>
     * <li>1.1 24/05/2016 Rafael Pizarro (TINet) - Claudia López (Ing. Soft. BCI): 
	 *     Se modifica retorno del metodo obtieneCotizacionesWSPrevired que ahora devuelve un objeto,
	 *     Se cambia el orden de actualización, primero se evaluan las reglas, despues se guarda el detalle.</li>
     * </ul>
     * </p>
     * @param rut rut del prospecto.
     * @param dv dv del prospecto.
     * @param canal canal de la consulta.
     * @param codServicio codigo servicio.
     * @param codigoViaje codigo del viaje del cliente.
     * @return true si la renta se actualiza correctamente, false en caso contrario.
     * @throws RemoteException Excepcion remota.
     * @throws Exception En caso de existir algún error.
     * @since 11.5
     */
    private boolean actualizarRentaProspectoCotizaciones(long rut, char dv, String canal, String codServicio,
        int codigoViaje) throws RemoteException, Exception {

        getLogger().info("[actualizarRentaProspectoCotizaciones] [BCI_INI]");
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("[actualizarRentaProspectoCotizaciones] [" + rut + "] rut= " + rut);
            getLogger().debug("[actualizarRentaProspectoCotizaciones] [" + rut + "] dv= " + dv);
            getLogger().debug("[actualizarRentaProspectoCotizaciones] [" + rut + "] canal= " + canal);
            getLogger().debug("[actualizarRentaProspectoCotizaciones] [" + rut + "] codServicio= " + codServicio);
        }

        String origenAct = TablaValores.getValor(TABLA_PARAMETROS_CALCULORENTA, "origenAct", "valor");
        String usuarioActualizacion = TablaValores.getValor(TABLA_PARAMETROS_CALCULORENTA, "usuarioActualizacion",
            "valor");

        CalculoRentaDAO dao = new CalculoRentaDAO();
        RentaTO renta = new RentaTO();
        renta.setRut(rut);
        renta.setDv(dv);
        renta.setCanal(canal);
        renta.setServicio(codServicio);
        renta.setCodigoViaje(codigoViaje);
        renta.setFechaActualizacion(new Date());
        renta.setOrigen(origenAct);
        renta.setUsuario(usuarioActualizacion);

        int codigoRegistro = -1;

        try {
            codigoRegistro = dao.ingresarEncabezadoCotizacionProspecto(renta);
            renta.setIdentificador(codigoRegistro);
            CotizacionPreviredTO cotizacionPreviredTO = dao.obtieneCotizacionesWSPrevired(rut, dv, codigoRegistro);
            CotizacionTO[] cotizacionesPrevired = cotizacionPreviredTO.getCotizaciones();
            renta.setCotizaciones(cotizacionesPrevired);

            if (getLogger().isDebugEnabled()) {
                getLogger().debug("[actualizarRentaProspectoCotizaciones] [" + rut + "] renta= " + renta);
            }
        }
        catch (Exception e) {
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[actualizarRentaProspectoCotizaciones] [" + rut + "] [Exception] [BCI_FINEX] ",
                    e);
            }
            if (codigoRegistro != -1) {
                String estNoCalculada = TablaValores.getValor(ARCHIVO_PARAMETROS, "EST_RENTA_PROBLEMAPREVIRED",
                    "valor");
                dao.actualizarEstadoEncabezadoCotizacionProspecto(codigoRegistro, estNoCalculada.charAt(0));
                if (getLogger().isEnabledFor(Level.ERROR)) {
                    getLogger().error(
                        "[actualizarRentaProspectoCotizaciones] [" + rut + "] [Exception] [BCI_FINEX] "
                            + "Error al obtener cotizaciones de previred, sin embargo el encabezado fué ingresado.",
                        e);
                }
                throw new GeneralException(ERROR_SIN_SERVICIO_PREVIRED);
            }
            throw new GeneralException(ERROR_INSERT_CABECERA);
        }

        try {
            getLogger().info("[actualizarRentaProspectoCotizaciones] Se evaluan reglas para prospecto.");
            renta = EvaluadorActualizacionRenta.evaluar(new CalculoRentaProspectoStrategy(renta, canal));
            
            if (renta == null) {
                if (getLogger().isEnabledFor(Level.ERROR)) {
                    getLogger()
                        .error("[actualizarRentaProspectoTransfer] [" + rut + "] [BCI_FINEX] renta es nula");
                }
                throw new GeneralException(ERROR_SIN_RENTA);
            }
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("[actualizarRentaProspectoCotizaciones] [" + rut + "] renta= " + renta);
            }
        }
        catch (Exception e) {
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[actualizarRentaProspectoCotizaciones] [" + rut + "] [Exception] [BCI_FINEX] "
                    + "No cumple evaluación de reglas, se cambia estado a encabezado cotizacion.", e);
            }
            String estNoCalculada = TablaValores.getValor(ARCHIVO_PARAMETROS, "EST_RENTA_NOCALCULADA", "valor");
            dao.actualizarEstadoEncabezadoCotizacionProspecto(codigoRegistro, estNoCalculada.charAt(0));

            throw e;
        }

        try {
            getLogger().info("[actualizarRentaProspectoCotizaciones] Se ingresa detalle de cotizaciones.");
            int cotizacionesInsertadas = dao.ingresoDetalleCotizacionProspecto(renta);
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("[actualizarRentaProspectoCotizaciones] [" + rut + "] cotizacionesInsertadas= "
                    + cotizacionesInsertadas);
            }
        }
        catch (Exception e) {
            if (getLogger().isEnabledFor(Level.WARN)) {
                getLogger().warn(
                    "[actualizarRentaProspectoCotizaciones] [" + rut + "] [Exception] [BCI_FINEX] "
                        + "Error al ingresar detalle de cotizaciones. Se continúa flujo por no ser inhabilitante",
                    e);
            }
        }

        try {
            getLogger().info("[actualizarRentaProspectoCotizaciones] Se ingresa renta calculada actualizada.");
            int registroRenta = dao.ingresarRentaCalculadaProspecto(renta);
            if (getLogger().isDebugEnabled()) {
                getLogger()
                    .debug("[actualizarRentaProspectoCotizaciones] [" + rut + "] registroRenta= " + registroRenta);
            }
        }
        catch (Exception e) {
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[actualizarRentaProspectoCotizaciones] [" + rut + "] [Exception] [BCI_FINEX] "
                    + "No se puede insertar renta actualizada.", e);
            }
            String estNoCalculada = TablaValores.getValor(ARCHIVO_PARAMETROS, "EST_RENTA_NOCALCULADA", "valor");
            dao.actualizarEstadoEncabezadoCotizacionProspecto(codigoRegistro, estNoCalculada.charAt(0));

            throw new GeneralException(ERROR_INSERT_RENTA_CALCULADA);
        }

        getLogger().info(
            "[actualizarRentaProspectoCotizaciones] renta actualizada, ahora se actualiza estado de encabezado.");
        String estadoActualizada = TablaValores.getValor(ARCHIVO_PARAMETROS, "EST_RENTA_ACTUALIZADA", "valor");
        dao.actualizarEstadoEncabezadoCotizacionProspecto(codigoRegistro, estadoActualizada.charAt(0));
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("[actualizarRentaProspectoCotizaciones] [BCI_FINOK] [" + rut + "] renta y estado "
                + "encabezado actualizado correctamente.");
        }

        return true;
    }
    
    /**
     * Método que actualiza la renta de un prospecto en función de los datos obtenidos desde transfer y luego de la 
     * aplicación de reglas definidas para estos efectos.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 15/03/2016 Ignacio González D. (TINet) - Oliver Hidalgo (Ing. Soft. BCI): Versión inicial.</li>
     * <li>1.1 22/04/2016 Ignacio González D. (TINet) - Oliver Hidalgo (Ing. Soft. BCI): Se ajustan fechas para
     * el filtro de búsqueda.</li>
     * </ul>
     * </p>
     * @param rut rut del prospecto.
     * @param dv dv del prospecto.
     * @param canal canal de la consulta.
     * @param codServicio codigo servicio.
     * @param codigoViaje codigo del viaje del cliente.
     * @return true si la renta se actualiza correctamente, false en caso contrario.
     * @throws RemoteException Excepcion remota.
     * @throws Exception En caso de existir algún error.
     * @since 11.3
     */
    private boolean actualizarRentaProspectoTransfer(long rut, char dv, String canal, String codServicio,
        int codigoViaje) throws RemoteException, Exception {

        getLogger().info("[actualizarRentaProspectoTransfer] [BCI_INI]");
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("[actualizarRentaProspectoTransfer] [" + rut + "] rut= " + rut);
            getLogger().debug("[actualizarRentaProspectoTransfer] [" + rut + "] dv= " + dv);
            getLogger().debug("[actualizarRentaProspectoTransfer] [" + rut + "] canal= " + canal);
            getLogger().debug("[actualizarRentaProspectoTransfer] [" + rut + "] codServicio= " + codServicio);
        }
        String origenAct = TablaValores.getValor(TABLA_PARAMETROS_CALCULORENTA, "origenTransfer", "valor");
        String usuarioActualizacion = TablaValores.getValor(TABLA_PARAMETROS_CALCULORENTA,
            "usuarioActualizacionTransfer", "valor");

        CalculoRentaDAO dao = new CalculoRentaDAO();
        RentaTO renta = new RentaTO();
        renta.setRut(rut);
        renta.setDv(dv);
        renta.setCanal(canal);
        renta.setServicio(codServicio);
        renta.setCodigoViaje(codigoViaje);
        renta.setFechaActualizacion(new Date());
        renta.setOrigen(origenAct);
        renta.setUsuario(usuarioActualizacion);

        try {
            FiltroConsultaPagoTO filtro = new FiltroConsultaPagoTO();
            filtro.setRutBeneficiario(rut);
            int cantidadPeriodos = Integer
                .parseInt(TablaValores.getValor(ReglasActualizacionRentaUtil.TABLA_CALCULO_RENTAS, "calculoRenta",
                    "cantidadPeriodosTransferCalculo"));
            filtro.setFechaPagoDesde(this.obtieneFechaMesesAnterior(cantidadPeriodos-1));
            filtro.setFechaPagoHasta(this.obtieneFechaUltimoDiaDelMesActual());
            filtro.setTipoPago("ALL");
            filtro.setFormaPago("ALL");

            PagoTO[] pagos = dao.consultaPagosBeneficiario(filtro);

            if (pagos == null || pagos.length == 0) {
                if (getLogger().isEnabledFor(Level.ERROR)) {
                    getLogger().error("[actualizarRentaProspectoTransfer] [" + rut + "] [Exception] [BCI_FINEX] "
                        + "no existen pagos disponibles para calculo de renta.");
                }
                return false;
            }
            renta.setPagos(pagos);
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("[actualizarRentaProspectoTransfer] [" + rut + "] renta= " + renta);
            }
        }
        catch (Exception e) {
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[actualizarRentaProspectoTransfer] [" + rut
                    + "] [Exception] [BCI_FINEX] Error al obtener pagos transfer:", e);
            }
            return false;
        }
        try {
            getLogger().info("[actualizarRentaProspectoTransfer] Se evaluan reglas para prospecto.");
            renta = EvaluadorActualizacionRenta.evaluar(new CalculoRentaProspectoTrfStrategy(renta, canal));
            
            if (renta == null) {
                if (getLogger().isEnabledFor(Level.ERROR)) {
                    getLogger()
                        .error("[actualizarRentaProspectoTransfer] [" + rut + "] [BCI_FINEX] renta es nula");
                }
                return false;
            }
            
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("[actualizarRentaProspectoTransfer] [" + rut + "] renta= " + renta);
            }
        }
        catch (Exception e) {
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[actualizarRentaProspectoTransfer] [" + rut + "] [Exception] [BCI_FINEX] "
                    + "No cumple evaluación de reglas.", e);
            }
            return false;
        }
        try {
            getLogger().info("[actualizarRentaProspectoTransfer] Se ingresa renta calculada actualizada.");
            int registroRenta = dao.ingresarRentaCalculadaProspecto(renta);
            if (getLogger().isDebugEnabled()) {
                getLogger()
                    .debug("[actualizarRentaProspectoTransfer] [" + rut + "] registroRenta= " + registroRenta);
            }
        }
        catch (Exception e) {
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[actualizarRentaProspectoTransfer] [" + rut + "] [Exception] [BCI_FINEX] "
                    + "No se puede insertar renta actualizada.", e);
            }
            return false;
        }
        return true;
    }
    
    /**
     * Método que permite obtener una fecha anterior en meses a partir de la fecha de hoy.
     * 
     * <p>
     * <b>Registro de versiones:</b>
     * <ul>
     * <li>1.0 15/03/2016 Ignacio González. (TINet) - Oliver Hidalgo (Ing. Soft. BCI): Versión inicial.</li>
     * <li>1.1 22/04/2016 Ignacio González. (TINet) - Oliver Hidalgo (Ing. Soft. BCI): Se ajusta método para que 
     * pueda retornar fecha con el primer día del mes calculado.</li>
     * </ul>
     * </p> 
     * 
     * @param meses cantidad de meses a retroceder en el calendario.
     * @return fecha anterior en meses  para continuar un cálculo pendiente.
     * @throws GeneralException en caso de no poder establecer la fecha solicitada.
     * @since 11.3
     */ 
    private Date obtieneFechaMesesAnterior(int meses) throws GeneralException {
        if (getLogger().isEnabledFor(Level.INFO)) {
            getLogger().info("[obtieneFechaMesesAnterior][BCI_INI] Iniciado.");
        }
        try {
            Calendar fechaDesde = Calendar.getInstance();
            fechaDesde.add(Calendar.MONTH, -meses);
            fechaDesde.set(fechaDesde.get(Calendar.YEAR),
                fechaDesde.get(Calendar.MONTH),
                fechaDesde.getActualMinimum(Calendar.DAY_OF_MONTH),
                fechaDesde.getMinimum(Calendar.HOUR_OF_DAY),
                fechaDesde.getMinimum(Calendar.MINUTE),
                fechaDesde.getMinimum(Calendar.SECOND)); 
            if (getLogger().isEnabledFor(Level.INFO)) {
                getLogger().info("[obtieneFechaMesesAnterior][BCI_FINOK] Finalizado.");
            }
            return fechaDesde.getTime();
        }
        catch (Exception e) {
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger()
                    .error("[obtieneFechaMesesAnterior][Exception][BCI_FINEX] error al obtener la fecha de un "
                        + "periodo anterior. " + e);
            }
            throw new GeneralException(ERROR_FECHA_ANTERIOR);
        }
    }
    
    /**
     * Método que permite obtener fecha con el último día del mes actual.
     * 
     * <p>
     * <b>Registro de versiones:</b>
     * <ul>
     * <li>1.0 22/04/2016 Ignacio González. (TINet) - Oliver Hidalgo (Ing. Soft. BCI): Versión inicial.</li>
     * </ul>
     * </p> 
     * 
     * @return fecha con el último día del mes actual.
     * @throws GeneralException en caso de no poder establecer la fecha solicitada.
     * @since 11.4
     */ 
    public Date obtieneFechaUltimoDiaDelMesActual() throws GeneralException {
        if (getLogger().isEnabledFor(Level.INFO)) {
            getLogger().info("[obtieneFechaUltimoDiaDelMesActual][BCI_INI] Iniciado.");
        }
        try {
            Calendar fechaHasta = Calendar.getInstance();
            fechaHasta.set(fechaHasta.get(Calendar.YEAR),
                           fechaHasta.get(Calendar.MONTH),
                           fechaHasta.getActualMaximum(Calendar.DAY_OF_MONTH),
                           fechaHasta.getMaximum(Calendar.HOUR_OF_DAY),
                           fechaHasta.getMaximum(Calendar.MINUTE),
                           fechaHasta.getMaximum(Calendar.SECOND));
            if (getLogger().isEnabledFor(Level.INFO)) {
                getLogger().info("[obtieneFechaUltimoDiaDelMesActual][BCI_FINOK] Finalizado.");
            }
            return fechaHasta.getTime();
        }
        catch (Exception e) {
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error(
                    "[obtieneFechaUltimoDiaDelMesActual][Exception][BCI_FINEX] error al obtener fecha "
                        + "solicitada. " + e);
            }
            throw new GeneralException(ERROR_FECHA_SOLICITADA);
        }
    }

    /**
     * Metodo que registra renta actualizada de acuerdo a reglas de sgc con previred.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 24/05/2016 Hernán Rodriguez(TINet) - Rafael Pizarro (TINet) - Claudia López (Ing. Soft. BCI): version inicial.
     * realiza un registro con los valores de las rentas considerando las reglas de sgc con previred.
     * renta fija dividida en miles.
     * </li>
     * </ul>
     * 
     * @param cotizacionCliente renta del cliente.
     * @throws Exception En caso de un error.
     * @return int resultado.
     * @since 11.5
     */
    public int ingresaRentaPreviredDGCRTA(CotizacionClienteVO cotizacionCliente) throws Exception {
        int resultado = 0;
        if (log.isDebugEnabled()) {
            log.debug("[ingresaRentaPreviredDGCRTA] En ingresarRegistroRTA");
        }
        try {
            CalculoRentaDAO calculoRentaDAO = new CalculoRentaDAO();
            if (log.isDebugEnabled()) {
                log.debug("[ingresaRentaPreviredDGCRTA]" + " Luego de instanciar el DAO[" + calculoRentaDAO + "]");
            }
            resultado = calculoRentaDAO.ingresaRentaPreviredDGCRTA(cotizacionCliente);
            if (log.isDebugEnabled()) {
                log.debug("[ingresaRentaPreviredDGCRTA]" + " resultado [" + resultado + "]");
            }
        }
        catch (Exception ex) {
            if (log.isEnabledFor(Level.ERROR)) {
                log.error("[ingresaRentaPreviredDGCRTA]:Error [" + ex.toString() + "]");
            }
            throw ex;
        }
        return resultado;
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
     * @since 12.0
     */
    public void ingresarRegistroActualizacionRenta(RegistroActualizaRentaTO registro) throws Exception{
        if (getLogger().isEnabledFor(Level.INFO)){
            getLogger().info("[ingresarRegistroActualizacionRenta][BCI_INI] registro: " + registro);
        }
        try{
            CalculoRentaDAO dao = new CalculoRentaDAO();
            dao.ingresarRegistroActualizacionRenta(registro);
        }
        catch(Exception e){
            if (getLogger().isEnabledFor(Level.ERROR)) {
                getLogger().error("[ingresarRegistroActualizacionRenta][Exception][BCI_FINEX]:"+e.getMessage(),e);
            }
            throw e;
        }
        if (getLogger().isEnabledFor(Level.INFO)){
            getLogger().info("[ingresarRegistroActualizacionRenta][BCI_FINOK]");
        }
    }
    
}

package br.com.swconsultoria.nfe;

import br.com.swconsultoria.certificado.exception.CertificadoException;
import br.com.swconsultoria.nfe.dom.ConfiguracoesNfe;
import br.com.swconsultoria.nfe.dom.enuns.DocumentoEnum;
import br.com.swconsultoria.nfe.dom.enuns.ServicosEnum;
import br.com.swconsultoria.nfe.exception.NfeException;
import br.com.swconsultoria.nfe.schema_rt.nfe.TConsReciNFe;
import br.com.swconsultoria.nfe.schema_rt.nfe.TRetConsReciNFe;
import br.com.swconsultoria.nfe.util.*;
import br.com.swconsultoria.nfe.wsdl.NFeRetAutorizacao.NFeRetAutorizacao4Stub;
import lombok.extern.java.Log;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axis2.transport.http.HTTPConstants;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import java.rmi.RemoteException;

/**
 * Classe Responsavel Por pegar o Retorno da NFE, apos o Envio.
 *
 * @author Samuel Oliveira
 */
@Log
class ConsultaReciboRT {

    private ConsultaReciboRT() {
    }

    /**
     * Metodo Responsavel Por Pegar o Xml De Retorno.
     *
     * @param config        Configuracoes
     * @param recibo        Número Do Recibo para Consulta
     * @param tipoDocumento Informe {@link DocumentoEnum}
     * @return
     * @throws NfeException
     */
    static TRetConsReciNFe reciboNfe(ConfiguracoesNfe config, String recibo, DocumentoEnum tipoDocumento) throws NfeException {

        try {

            /**
             * Informaçoes do Certificado Digital.
             */

            TConsReciNFe consReciNFe = new TConsReciNFe();
            consReciNFe.setVersao(ConstantesUtil.VERSAO.NFE);
            consReciNFe.setTpAmb(config.getAmbiente().getCodigo());
            consReciNFe.setNRec(recibo);

            String xml = XmlNfeUtil.objectToXml(consReciNFe, config.getEncode());

            log.info("[XML-ENVIO]: " + xml);

            OMElement ome = AXIOMUtil.stringToOM(xml);
            NFeRetAutorizacao4Stub.NfeDadosMsg dadosMsg = new NFeRetAutorizacao4Stub.NfeDadosMsg();
            dadosMsg.setExtraElement(ome);

            String url = WebServiceUtil.getUrl(config, tipoDocumento, ServicosEnum.CONSULTA_RECIBO);
            NFeRetAutorizacao4Stub stub = new NFeRetAutorizacao4Stub(url);

            StubUtil.configuraHttpClient(stub, config, url);

            // Timeout
            if (ObjetoUtil.verifica(config.getTimeout()).isPresent()) {
                stub._getServiceClient().getOptions().setProperty(HTTPConstants.SO_TIMEOUT, config.getTimeout());
                stub._getServiceClient().getOptions().setProperty(HTTPConstants.CONNECTION_TIMEOUT,
                        config.getTimeout());
            }
            NFeRetAutorizacao4Stub.NfeResultMsg result = stub.nfeRetAutorizacaoLote(dadosMsg);

            log.info("[XML-RETORNO]: " + result.getExtraElement().toString());
            return XmlNfeUtil.xmlToObject(result.getExtraElement().toString(), TRetConsReciNFe.class);

        } catch (RemoteException | XMLStreamException | JAXBException | CertificadoException e) {
            throw new NfeException(e.getMessage(), e);
        }

    }
}

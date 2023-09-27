package net.trellisframework.message.task;

import net.trellisframework.message.config.MessageProperties;
import net.trellisframework.message.constant.MagfaMessages;
import net.trellisframework.message.payload.SendMessageResponse;
import org.apache.axis.client.Call;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import javax.xml.namespace.QName;
import javax.xml.rpc.ParameterMode;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SendBatchSmsWithMagfaTask extends AbstractSendBatchSmsTask {

    @Override
    public List<SendMessageResponse> execute(MessageProperties.SmsPropertiesDefinition configuration, List<String> recipients, String message) {
        try {
            final String END_POINT_URL = "https://sms.magfa.com/services/urn:SOAPSmsQueue";
            final String URN = "urn:SOAPSmsQueue";
            final String ENQUEUE_METHOD_CALL = "enqueue";
            org.apache.axis.client.Service service = new org.apache.axis.client.Service();
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress(new URL(END_POINT_URL));
            call.setOperationName(new QName(URN, ENQUEUE_METHOD_CALL));
            call.setUsername(configuration.getUsername());
            call.setPassword(configuration.getPassword());
            call.setReturnType(org.apache.axis.encoding.XMLType.SOAP_ARRAY);
            call.setTimeout(60 * 1000);
            call.addParameter("domain", org.apache.axis.encoding.XMLType.SOAP_STRING, ParameterMode.IN);
            call.addParameter("messages", org.apache.axis.encoding.XMLType.SOAP_ARRAY, ParameterMode.IN);
            call.addParameter("recipientNumbers", org.apache.axis.encoding.XMLType.SOAP_ARRAY, ParameterMode.IN);
            call.addParameter("senderNumbers", org.apache.axis.encoding.XMLType.SOAP_ARRAY, ParameterMode.IN);
            String domain = configuration.getDomain();
            String[] messages = new String[]{message};
            String[] recipientNumbers = recipients.toArray(String[]::new);
            String[] senderNumbers = new String[]{configuration.getFrom()};
            Object[] params = {domain, messages, recipientNumbers, senderNumbers};
            Long[] results = (Long[]) call.invoke(params);
            if (ObjectUtils.isEmpty(results))
                return null;
            return Stream.of(results).map(x -> MagfaMessages.isSuccess(x) ?SendMessageResponse.ok() :  SendMessageResponse.error(MagfaMessages.of(x).getMessage())).collect(Collectors.toList());
        } catch (Exception e) {
            System.out.println("Magfa error code: " + e.getMessage());
            return null;
        }
    }

}

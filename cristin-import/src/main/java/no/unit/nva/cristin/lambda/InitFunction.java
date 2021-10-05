package no.unit.nva.cristin.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@JacocoGenerated
public class InitFunction implements RequestStreamHandler {

    private final static Logger logger= LoggerFactory.getLogger(InitFunction.class);

    @JacocoGenerated
    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        SimpleMessage simpleMessage = new SimpleMessage("hello");
        String messageString= JsonUtils.objectMapperWithEmpty.writeValueAsString(simpleMessage);
        logger.info(messageString);
        writeOutput(messageString,output);
    }

    @JacocoGenerated
    private void writeOutput(String messageString, OutputStream outputStream) throws IOException {
        try(BufferedWriter writer=new BufferedWriter(new OutputStreamWriter(outputStream))){
          writer.write(messageString);
        }
    }
}

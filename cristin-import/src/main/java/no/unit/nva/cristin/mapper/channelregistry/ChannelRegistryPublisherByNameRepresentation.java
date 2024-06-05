package no.unit.nva.cristin.mapper.channelregistry;

import com.opencsv.bean.CsvBindByName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChannelRegistryPublisherByNameRepresentation {

    @CsvBindByName(column = "Original tittel")
    private String title;

    @CsvBindByName(column = "PID")
    private String pid;

    public boolean hasTitle(String value) {
        return title.equalsIgnoreCase(value);
    }
}

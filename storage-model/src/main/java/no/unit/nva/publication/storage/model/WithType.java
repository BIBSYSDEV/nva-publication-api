package no.unit.nva.publication.storage.model;

public interface WithType {

     String getType();

     default void setType(){
         // do nothing
     }
}

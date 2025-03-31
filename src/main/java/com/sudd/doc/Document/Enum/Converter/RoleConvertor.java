package com.sudd.doc.Document.Enum.Converter;

import java.util.stream.Stream;

import com.sudd.doc.Document.Enum.Authority;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
// takes enum , type 
@Converter(autoApply = true)
public class RoleConvertor implements AttributeConverter<Authority , String> {

    @Override
    public String convertToDatabaseColumn(Authority authority) {
      if(authority==null){
        return null;
      }
      return authority.getValue();
    }

    @Override
    public Authority convertToEntityAttribute(String code) {
        if(code == null){
            return null;
        }
        return Stream.of(Authority.values()).filter(authority->authority.getValue().equals(code))
        .findFirst()
        .orElseThrow(IllegalArgumentException::new);
    }
    
}

package com.ecovolt.app;

import com.ecovolt.model.CustomerBill;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

public class JsonExporter {

    private final ObjectMapper mapper;

    public JsonExporter() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void exportBillsToJson(Collection<CustomerBill> bills, String filePath) throws IOException {
        mapper.writeValue(new File(filePath), bills);
    }
}


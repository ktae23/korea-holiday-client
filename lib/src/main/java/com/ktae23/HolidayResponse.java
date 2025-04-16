package com.ktae23;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HolidayResponse {
    public Response response;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response {
        public Body body;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        public Items items;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Items {
        public List<Item> item;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        @JsonProperty("locdate")
        public String localDate;

        @JsonProperty("dateName")
        public String dateName;

        @JsonProperty("isHoliday")
        public String isHoliday;
    }
}
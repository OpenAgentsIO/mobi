package org.matonto.etl.api.config;

import org.matonto.rdf.api.Model;

import java.io.InputStream;

public class SVConfig {
    private InputStream data;
    private Model mapping;
    private boolean containsHeaders = true;
    private char separator = ',';
    private long limit = 0;
    private long offset = 0;

    private SVConfig(Builder builder) {
        data = builder.data;
        mapping = builder.mapping;
        containsHeaders = builder.containsHeaders;
        separator = builder.separator;
        limit = builder.limit;
        offset = builder.offset;
    }

    public InputStream getData() {
        return data;
    }

    public Model getMapping() {
        return mapping;
    }

    public boolean getContainsHeaders() {
        return containsHeaders;
    }

    public char getSeparator() {
        return separator;
    }

    public long getLimit() {
        return limit;
    }

    public long getOffset() {
        return offset;
    }

    public static class Builder {
        private InputStream data;
        private Model mapping;
        private boolean containsHeaders = true;
        private char separator = ',';
        private long limit = 0;
        private long offset = 0;

        public Builder(InputStream data, Model mapping) {
            this.data = data;
            this.mapping = mapping;
        }

        public Builder containsHeaders(boolean containsHeaders) {
            this.containsHeaders = containsHeaders;
            return this;
        }

        public Builder separator(char separator) {
            this.separator = separator;
            return this;
        }

        public Builder limit(long limit) {
            this.limit = limit;
            return this;
        }

        public Builder offset(long offset) {
            this.offset = offset;
            return this;
        }

        public SVConfig build() {
            return new SVConfig(this);
        }
    }
}

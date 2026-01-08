package com.pe.plugin.avro.schema;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonStringFormatVisitor;
import com.fasterxml.jackson.dataformat.avro.schema.AvroSchemaGenerator;
import com.fasterxml.jackson.dataformat.avro.schema.StringVisitor;
import com.fasterxml.jackson.dataformat.avro.schema.VisitorFormatWrapperImpl;
import org.apache.avro.LogicalType;
import org.apache.avro.Schema;

import java.util.UUID;

/**
 *
 * @author kamen on 08.01.2026
 */
public class CustomAvroSchemaGenerator extends AvroSchemaGenerator {

    public CustomAvroSchemaGenerator() {
        enableLogicalTypes();
    }

    @Override
    public JsonStringFormatVisitor expectStringFormat(JavaType type) {
        // implementation copied from VisitorFormatWrapperImpl,
        // except for replacing StringVisitor with UpStringVisitor
        final Schema s = _schemas.findSchema(type);
        if (s != null) {
            _valueSchema = s;
            return null;
        }
        final var v = new UpStringVisitor(_provider, type);
        _builder = v;
        return v;
    }

    @Override
    protected VisitorFormatWrapperImpl createChildWrapper() {
        return new UpVisitorFormatWrapperImpl(this);
    }

    private static class UpVisitorFormatWrapperImpl extends VisitorFormatWrapperImpl {
        protected UpVisitorFormatWrapperImpl(VisitorFormatWrapperImpl src) {
            super(src);
        }

        @Override
        public JsonStringFormatVisitor expectStringFormat(JavaType type) {
            // implementation copied from VisitorFormatWrapperImpl,
            // except for replacing StringVisitor with UpStringVisitor
            final Schema s = _schemas.findSchema(type);
            if (s != null) {
                _valueSchema = s;
                return null;
            }
            final var v = new UpStringVisitor(_provider, type);
            _builder = v;
            return v;
        }

        @Override
        protected VisitorFormatWrapperImpl createChildWrapper() {
            return new UpVisitorFormatWrapperImpl(this);
        }
    }

    private static class UpStringVisitor extends StringVisitor {
        UpStringVisitor(SerializerProvider provider, JavaType t) {
            super(provider, t);
        }

        @Override
        public Schema builtAvroSchema() {
            // Jackson's default implementation maps UUID to a custom type.
            // We instead use the Avro standard, which is a string with logical type "uuid".
            if (_type.hasRawClass(UUID.class)) {
                Schema schema = Schema.create(Schema.Type.STRING);
                schema.addProp(LogicalType.LOGICAL_TYPE_PROP, "uuid");
                return schema;
            }

            return super.builtAvroSchema();
        }
    }
}


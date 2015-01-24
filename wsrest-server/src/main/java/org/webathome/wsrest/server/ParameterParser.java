package org.webathome.wsrest.server;

import com.google.gson.Gson;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class ParameterParser {
    public static ParameterParser jsonParser(Class<?> type, Type genericType) {
        return new JsonParser(type, genericType);
    }

    public static ParameterParser xmlParser() throws WsRestException {
        throw new WsRestException("XML types have not been implemented");
    }

    public static ParameterParser textParser() {
        return new SingleItemParser(new StringParser());
    }

    public static ParameterParser valueParser(Class<?> type, Type genericType) throws WsRestException {
        Class<?> itemType = null;
        if (type.isArray()) {
            itemType = type.getComponentType();
        } else if (genericType instanceof ParameterizedType) {
            Type[] typeArguments = ((ParameterizedType)genericType).getActualTypeArguments();
            if (typeArguments != null && typeArguments.length == 1) {
                itemType = typeArguments[0].getClass();
            }
            if (itemType == null) {
                throw new WsRestException("Cannot determine parameter type");
            }
        } else {
            itemType = type;
            type = null;
        }

        ItemParser itemParser;

        if (itemType == Character.TYPE) {
            itemParser = new CharacterParser(false);
        } else if (itemType == Character.class) {
            itemParser = new CharacterParser(true);
        } else if (itemType == Short.TYPE) {
            itemParser = new ShortParser(false);
        } else if (itemType == Short.class) {
            itemParser = new ShortParser(true);
        } else if (itemType == Integer.TYPE) {
            itemParser = new IntegerParser(false);
        } else if (itemType == Integer.class) {
            itemParser = new IntegerParser(true);
        } else if (itemType == Long.TYPE) {
            itemParser = new LongParser(false);
        } else if (itemType == Long.class) {
            itemParser = new LongParser(true);
        } else if (itemType == Boolean.TYPE) {
            itemParser = new BooleanParser(false);
        } else if (itemType == Boolean.class) {
            itemParser = new BooleanParser(true);
        } else if (itemType == Float.TYPE) {
            itemParser = new FloatParser(false);
        } else if (itemType == Float.class) {
            itemParser = new FloatParser(true);
        } else if (itemType == Double.TYPE) {
            itemParser = new DoubleParser(false);
        } else if (itemType == Double.class) {
            itemParser = new DoubleParser(true);
        } else if (itemType == String.class) {
            itemParser = new StringParser();
        } else {
            throw new WsRestException(String.format("Cannot parse %s", itemType.getName()));
        }

        if (type == null) {
            return new SingleItemParser(itemParser);
        }

        if (type.isArray()) {
            return new ArrayParser(itemParser, itemType);
        }
        if (type == ArrayList.class || type == List.class || type == Collection.class) {
            return new ArrayListParser(itemParser);
        }

        throw new WsRestException(String.format("Cannot create parser for %s", type.getName()));
    }

    public abstract Object encode(Object value) throws WsRestException;

    public abstract Object decode(Object value) throws WsRestException;

    private static class SingleItemParser extends ParameterParser {
        private final ItemParser itemParser;

        public SingleItemParser(ItemParser itemParser) {
            this.itemParser = itemParser;
        }

        @Override
        public Object encode(Object value) throws WsRestException {
            return itemParser.encode(value);
        }

        @Override
        public Object decode(Object value) throws WsRestException {
            if (value instanceof String[]) {
                String[] array = (String[])value;

                switch (array.length) {
                    case 0:
                        value = null;
                        break;

                    case 1:
                        value = array[0];
                        break;

                    default:
                        throw new WsRestException("Parameter does not accept a list");
                }
            }

            return itemParser.decode((String)value);
        }
    }

    private static class ArrayParser extends ParameterParser {
        private final ItemParser itemParser;
        private final Class<?> itemType;

        public ArrayParser(ItemParser itemParser, Class<?> itemType) {
            this.itemParser = itemParser;
            this.itemType = itemType;
        }

        @Override
        public Object encode(Object value) throws WsRestException {
            if (value == null) {
                return null;
            }

            String[] result = new String[Array.getLength(value)];

            for (int i = 0; i < result.length; i++) {
                result[i] = itemParser.encode(Array.get(value, i));
            }

            return result;
        }

        @Override
        public Object decode(Object value) throws WsRestException {
            if (value == null) {
                return null;
            }

            String[] array;
            if (value instanceof String) {
                array = new String[]{(String)value};
            } else {
                array = (String[])value;
            }

            Object result = Array.newInstance(itemType, array.length);

            for (int i = 0; i < array.length; i++) {
                Array.set(result, i, itemParser.decode(array[i]));
            }

            return result;
        }
    }

    private static class ArrayListParser extends ParameterParser {
        private final ItemParser itemParser;

        public ArrayListParser(ItemParser itemParser) {
            this.itemParser = itemParser;
        }

        @Override
        public Object encode(Object value) throws WsRestException {
            if (value == null) {
                return null;
            }

            List list = (List)value;
            String[] result = new String[list.size()];

            for (int i = 0; i < list.size(); i++) {
                result[i] = itemParser.encode(list.get(i));
            }

            return result;
        }

        @SuppressWarnings({"unchecked", "ForLoopReplaceableByForEach"})
        @Override
        public Object decode(Object value) throws WsRestException {
            if (value == null) {
                return null;
            }

            String[] array;
            if (value instanceof String) {
                array = new String[]{(String)value};
            } else {
                array = (String[])value;
            }

            List result = new ArrayList(array.length);
            for (int i = 0; i < array.length; i++) {
                result.add(itemParser.decode(array[i]));
            }

            return result;
        }
    }

    private abstract static class ItemParser {
        public abstract String encode(Object value) throws WsRestException;

        public abstract Object decode(String value) throws WsRestException;
    }

    private static class CharacterParser extends ItemParser {
        private final boolean nullable;

        public CharacterParser(boolean nullable) {
            this.nullable = nullable;
        }

        @Override
        public String encode(Object value) throws WsRestException {
            if (value == null) {
                return null;
            }

            return String.valueOf((char)value);
        }

        @Override
        public Object decode(String value) throws WsRestException {
            if (value == null) {
                return nullable ? null : (char)0;
            }

            if (value.length() != 1) {
                throw new WsRestException("Unexpected character argument");
            }
            return value.charAt(0);
        }
    }

    private static class ShortParser extends ItemParser {
        private final boolean nullable;

        public ShortParser(boolean nullable) {
            this.nullable = nullable;
        }

        @Override
        public String encode(Object value) throws WsRestException {
            if (value == null) {
                return null;
            }

            return String.valueOf((short)value);
        }

        @Override
        public Object decode(String value) throws WsRestException {
            if (value == null) {
                return nullable ? null : (short)0;
            }

            return Short.parseShort(value);
        }
    }

    private static class IntegerParser extends ItemParser {
        private final boolean nullable;

        public IntegerParser(boolean nullable) {
            this.nullable = nullable;
        }

        @Override
        public String encode(Object value) throws WsRestException {
            if (value == null) {
                return null;
            }

            return String.valueOf((int)value);
        }

        @Override
        public Object decode(String value) throws WsRestException {
            if (value == null) {
                return nullable ? null : (int)0;
            }

            return Integer.parseInt(value);
        }
    }

    private static class LongParser extends ItemParser {
        private final boolean nullable;

        public LongParser(boolean nullable) {
            this.nullable = nullable;
        }

        @Override
        public String encode(Object value) throws WsRestException {
            if (value == null) {
                return null;
            }

            return String.valueOf((long)value);
        }

        @Override
        public Object decode(String value) throws WsRestException {
            if (value == null) {
                return nullable ? null : (long)0;
            }

            return Long.parseLong(value);
        }
    }

    private static class FloatParser extends ItemParser {
        private final boolean nullable;

        public FloatParser(boolean nullable) {
            this.nullable = nullable;
        }

        @Override
        public String encode(Object value) throws WsRestException {
            if (value == null) {
                return null;
            }

            return String.valueOf((float)value);
        }

        @Override
        public Object decode(String value) throws WsRestException {
            if (value == null) {
                return nullable ? null : (float)0;
            }

            return Float.parseFloat(value);
        }
    }

    private static class DoubleParser extends ItemParser {
        private final boolean nullable;

        public DoubleParser(boolean nullable) {
            this.nullable = nullable;
        }

        @Override
        public String encode(Object value) throws WsRestException {
            if (value == null) {
                return null;
            }

            return String.valueOf((double)value);
        }

        @Override
        public Object decode(String value) throws WsRestException {
            if (value == null) {
                return nullable ? null : (double)0;
            }

            return Double.parseDouble(value);
        }
    }

    private static class BooleanParser extends ItemParser {
        private final boolean nullable;

        public BooleanParser(boolean nullable) {
            this.nullable = nullable;
        }

        @Override
        public String encode(Object value) throws WsRestException {
            if (value == null) {
                return null;
            }

            return (boolean)value ? "true" : "false";
        }

        @Override
        public Object decode(String value) throws WsRestException {
            if (value == null) {
                return nullable ? null : false;
            }

            switch (value) {
                case "false":
                case "0":
                    return false;

                case "true":
                case "1":
                    return true;

                default:
                    throw new WsRestException(String.format(
                        "Cannot parse %s as boolean",
                        value
                    ));
            }
        }
    }

    private static class StringParser extends ItemParser {
        @Override
        public String encode(Object value) throws WsRestException {
            return (String)value;
        }

        @Override
        public Object decode(String value) throws WsRestException {
            return value;
        }
    }

    private static class JsonParser extends ParameterParser {
        private static final Gson GSON = new Gson();
        private final Class<?> type;
        private final Type genericType;

        public JsonParser(Class<?> type, Type genericType) {
            this.type = type;
            this.genericType = genericType;
        }

        @Override
        public Object encode(Object value) throws WsRestException {
            return GSON.toJson(value, genericType);
        }

        @Override
        public Object decode(Object value) throws WsRestException {
            if (value == null) {
                return null;
            }

            if (genericType != null) {
                return GSON.fromJson((String)value, genericType);
            }

            return GSON.fromJson((String)value, type);
        }
    }
}

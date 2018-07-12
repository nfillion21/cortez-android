package com.tezos.core.models;
import android.os.Bundle;

import com.tezos.core.mapper.AbstractMapper;
import com.tezos.core.mapper.interfaces.MapMapper;
import com.tezos.core.serialization.AbstractSerializationMapper;
import com.tezos.core.serialization.interfaces.AbstractSerialization;

import org.json.JSONObject;

import java.util.Map;

public class Operation extends AbstractModel
{
    public static final String TAG = "Operation";

    protected String hash;
    protected Integer operationId;
    protected String blockHash;
    protected String timestamp;
    protected String source;
    protected String sourceManager;
    protected String destination;
    protected String destinationManager;
    protected Float amount;

    protected Float fee;

    public Operation() {}

    public Bundle toBundle()
    {
        OperationSerializationMapper mapper = new OperationSerializationMapper(this);
        return mapper.getSerializedBundle();
    }

    public static Operation fromBundle(Bundle bundle)
    {
        OperationMapper mapper = new OperationMapper(bundle);
        return mapper.mappedObjectFromBundle();
    }

    public static Operation fromJSONObject(JSONObject object) {

        OperationMapper mapper = new OperationMapper(object);
        return mapper.mappedObjectFromJSON();
    }

    public static class OperationSerializationMapper extends AbstractSerializationMapper
    {
        protected OperationSerializationMapper(Operation operation)
        {
            super(operation);
        }

        @Override
        protected String getQueryString()
        {
            return super.getQueryString();
        }

        @Override
        protected Bundle getSerializedBundle()
        {
            return super.getSerializedBundle();
        }
    }

    protected static class OperationMapper extends AbstractMapper
    {
        public OperationMapper(Object rawData)
        {
            super(rawData);
        }

        @Override
        protected boolean isValid()
        {
            if (this.getBehaviour() instanceof MapMapper)
            {
                //if (this.getStringForKey("enrollmentStatus") != null) return true;
            }

            //return false;

            return true;
        }

        public Operation mappedObjectFromJSON()
        {
            Operation object = new Operation();

            object.setHash(this.getStringForKey("hash"));
            object.setOperationId(this.getIntegerForKey("operationId"));
            object.setBlockHash(this.getStringForKey("blockHash"));
            object.setTimestamp(this.getStringForKey("timestamp"));
            object.setSource(this.getStringForKey("source"));
            object.setSourceManager(this.getStringForKey("sourceManager"));
            object.setDestination(this.getStringForKey("destination"));
            object.setDestinationManager(this.getStringForKey("destinationManager"));
            object.setAmount(this.getFloatForKey("amount"));
            object.setFee(this.getFloatForKey("fee"));

            return object;
        }

        @Override
        public Operation mappedObjectFromBundle()
        {
            Operation object = new Operation();

            object.setHash(this.getStringForKey("hash"));
            object.setOperationId(this.getIntegerForKey("operationId"));
            object.setBlockHash(this.getStringForKey("blockHash"));
            object.setTimestamp(this.getStringForKey("timestamp"));
            object.setSource(this.getStringForKey("source"));
            object.setSourceManager(this.getStringForKey("sourceManager"));
            object.setDestination(this.getStringForKey("destination"));
            object.setDestinationManager(this.getStringForKey("destinationManager"));
            object.setAmount(this.getFloatForKey("amount"));
            object.setFee(this.getFloatForKey("fee"));

            return object;
        }
    }

    public static class OperationSerialization extends AbstractSerialization
    {
        public OperationSerialization(Address address)
        {
            super(address);
        }

        @Override
        public Map<String, String> getSerializedRequest()
        {
            return null;
        }

        @Override
        public Bundle getSerializedBundle()
        {
            super.getSerializedBundle();

            Operation operation = (Operation) this.getModel();
            this.putStringForKey("hash", operation.getHash());
            this.putIntForKey("operationId", operation.getOperationId());
            this.putStringForKey("blockHash", operation.getBlockHash());
            this.putStringForKey("timestamp", operation.getTimestamp());
            this.putStringForKey("source", operation.getSource());
            this.putStringForKey("sourceManager", operation.getSourceManager());
            this.putStringForKey("destination", operation.getDestination());
            this.putStringForKey("destinationManager", operation.getDestinationManager());
            this.putFloatForKey("amount", operation.getAmount());
            this.putFloatForKey("fee", operation.getFee());

            return this.getBundle();
        }

        @Override
        public String getQueryString()
        {
            return null;
        }
    }


    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public Integer getOperationId() {
        return operationId;
    }

    public void setOperationId(Integer operationId) {
        this.operationId = operationId;
    }

    public String getBlockHash() {
        return blockHash;
    }

    public void setBlockHash(String blockHash) {
        this.blockHash = blockHash;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceManager() {
        return sourceManager;
    }

    public void setSourceManager(String sourceManager) {
        this.sourceManager = sourceManager;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getDestinationManager() {
        return destinationManager;
    }

    public void setDestinationManager(String destinationManager) {
        this.destinationManager = destinationManager;
    }

    public Float getAmount() {
        return amount;
    }

    public void setAmount(Float amount) {
        this.amount = amount;
    }

    public Float getFee() {
        return fee;
    }

    public void setFee(Float fee) {
        this.fee = fee;
    }
}

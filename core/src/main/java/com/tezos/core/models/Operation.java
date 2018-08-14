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

            object.setHash(this.getStringForKey(HASH_KEY));
            object.setOperationId(this.getIntegerForKey(OPERATION_ID_KEY));
            object.setBlockHash(this.getStringForKey(BLOCKHASH_KEY));
            object.setTimestamp(this.getStringForKey(TIMESTAMP_KEY));
            object.setSource(this.getStringForKey(SOURCE_KEY));
            object.setSourceManager(this.getStringForKey(SOURCE_MANAGER_KEY));
            object.setDestination(this.getStringForKey(DESTINATION_KEY));
            object.setDestinationManager(this.getStringForKey(DESTINATION_MANAGER_KEY));
            object.setAmount(this.getFloatForKey(AMOUNT_KEY));
            object.setFee(this.getFloatForKey(FEE_KEY));

            return object;
        }

        @Override
        public Operation mappedObjectFromBundle()
        {
            Operation object = new Operation();

            object.setHash(this.getStringForKey(HASH_KEY));
            object.setOperationId(this.getIntegerForKey(OPERATION_ID_KEY));
            object.setBlockHash(this.getStringForKey(BLOCKHASH_KEY));
            object.setTimestamp(this.getStringForKey(TIMESTAMP_KEY));
            object.setSource(this.getStringForKey(SOURCE_KEY));
            object.setSourceManager(this.getStringForKey(SOURCE_MANAGER_KEY));
            object.setDestination(this.getStringForKey(DESTINATION_KEY));
            object.setDestinationManager(this.getStringForKey(DESTINATION_MANAGER_KEY));
            object.setAmount(this.getFloatForKey(AMOUNT_KEY));
            object.setFee(this.getFloatForKey(FEE_KEY));

            return object;
        }
    }

    public static class OperationSerialization extends AbstractSerialization
    {
        public OperationSerialization(Operation operation)
        {
            super(operation);
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
            this.putStringForKey(HASH_KEY, operation.getHash());
            this.putIntForKey(OPERATION_ID_KEY, operation.getOperationId());
            this.putStringForKey(BLOCKHASH_KEY, operation.getBlockHash());
            this.putStringForKey(TIMESTAMP_KEY, operation.getTimestamp());
            this.putStringForKey(SOURCE_KEY, operation.getSource());
            this.putStringForKey(SOURCE_MANAGER_KEY, operation.getSourceManager());
            this.putStringForKey(DESTINATION_KEY, operation.getDestination());
            this.putStringForKey(DESTINATION_MANAGER_KEY, operation.getDestinationManager());
            this.putFloatForKey(AMOUNT_KEY, operation.getAmount());
            this.putFloatForKey(FEE_KEY, operation.getFee());

            return this.getBundle();
        }

        @Override
        public String getQueryString()
        {
            return null;
        }
    }

    public static final String HASH_KEY = "op_hash";
    public static final String OPERATION_ID_KEY = "id";
    public static final String BLOCKHASH_KEY = "blk_hash";
    public static final String TIMESTAMP_KEY = "timestamp";
    public static final String SOURCE_KEY = "src";
    public static final String SOURCE_MANAGER_KEY = "src_mgr";
    public static final String DESTINATION_KEY = "dst";
    public static final String DESTINATION_MANAGER_KEY = "dst_mgr";
    public static final String AMOUNT_KEY = "amount";
    public static final String FEE_KEY = "fee";


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

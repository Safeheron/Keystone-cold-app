package com.keystone.coinlib.coins.MPC.model;

import com.google.gson.Gson;
import java.io.Serializable;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author yudenghao
 * @date 2022/7/26
 */

public class TransactionDetails implements Serializable {

  public String from = "";

  public String message1;

  public TxObj txObj;

  public String networkName;

  public static class TxObj implements Serializable {

    public String gasLimit;

    public String maxFeePerGas = "";

    public String to;

    public String value;

    public int chainId;

    public int nonce;

    public String data;

    public String maxPriorityFeePerGas = "";
  }

  public TransactionDetails jsonToTransactionDetails(String json) {
    return new Gson().fromJson(json, TransactionDetails.class);
  }

  public String toJson() {
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("nonce", txObj.nonce);
      jsonObject.put("to", txObj.to);
      jsonObject.put("value", txObj.value);
      jsonObject.put("chainId", txObj.chainId);
      jsonObject.put("data", txObj.data);
      jsonObject.put("maxPriorityFeePerGas", txObj.maxPriorityFeePerGas);
      jsonObject.put("maxFeePerGas", txObj.maxFeePerGas);
      jsonObject.put("gasLimit", txObj.gasLimit);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return jsonObject.toString();
  }

  @Override
  public String toString() {
    return "\"TransactionDetails\": {"
        + "\"from\": \""
        + from
        + '\"'
        + ", \"message1\": \""
        + message1
        + '\"'
        + ", \"txObj\": \""
        + txObj
        + ", \"networkName\": \""
        + networkName
        + '\"'
        + '}';
  }
}

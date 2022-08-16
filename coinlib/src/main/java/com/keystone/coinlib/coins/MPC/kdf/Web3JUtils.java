package com.keystone.coinlib.coins.MPC.kdf;

import android.util.Log;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.crypto.generators.SCrypt;
import org.json.JSONObject;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Wallet;
import org.web3j.crypto.WalletFile;
import org.web3j.utils.Numeric;

/**
 * @author yudenghao
 * @date 2022/8/8
 */

public class Web3JUtils {

  private static final String TAG = "Web3JUtils";

  public Web3JCallback mWeb3JCallback;

  public Web3JUtils(Web3JCallback web3JCallback) {
    this.mWeb3JCallback = web3JCallback;
  }

  public String generateWalletFile(String password, String address, String keyShard,
      String destinationDirectory, String fileName) {
    try {
      if (mWeb3JCallback != null) {
        mWeb3JCallback.onStart();
      }
      int n = 1 << 17;
      int p = 1;
      byte[] bytes = keyShard.getBytes();
      byte[] salt = generateRandomBytes(32);
      byte[] derivedKey = generateDerivedScryptKey(password.getBytes(StandardCharsets.UTF_8), salt, n, 8, p, 32);
      byte[] encryptKey = Arrays.copyOfRange(derivedKey, 0, 16);
      byte[] iv = generateRandomBytes(16);
      byte[] cipherText = performCipherOperation(1, iv, encryptKey, bytes);
      byte[] mac = generateMac(derivedKey, cipherText);
      WalletFile walletFile = createWalletFile(address, cipherText, iv, salt, mac, n, p);
      String path = writeWalletFile(walletFile, destinationDirectory, fileName);
      if (mWeb3JCallback != null) {
        mWeb3JCallback.onSuccess(path);
      }
      return path;
    } catch (Exception e) {
      if (mWeb3JCallback != null) {
        mWeb3JCallback.onError(e.getMessage());
      }
      return null;
    }
  }

  public String getWalletJson(String password, String filePath) {
    if (mWeb3JCallback != null) {
      mWeb3JCallback.onStart();
    }
    try {
      WalletFile walletFile = jsonFile2WalletFile(filePath);
      ECKeyPair eCKeyPair = Wallet.decrypt(password, walletFile);
      String res = getPrivateKey(eCKeyPair);
      if (mWeb3JCallback != null) {
        mWeb3JCallback.onSuccess(res);
      }
      return res;
    } catch (CipherException e) {
      if (mWeb3JCallback != null) {
        mWeb3JCallback.onError(e.getMessage());
      }
      return null;
    }
  }

  private String writeWalletFile(WalletFile walletFile, String destinationDirectory,
      String fileName) {
    ObjectMapper objectMapper = new ObjectMapper();
    File destination = new File(destinationDirectory, fileName + ".json");
    try {
      objectMapper.writeValue(destination, walletFile);
    } catch (Exception e) {
      Log.e(TAG, Objects.requireNonNull(e.getMessage()));
    }
    return destination.getAbsolutePath();
  }

  private WalletFile createWalletFile(String address, byte[] cipherText, byte[] iv, byte[] salt,
      byte[] mac, int n, int p) {
    WalletFile walletFile = new WalletFile();
    walletFile.setAddress(address);
    WalletFile.Crypto crypto = new WalletFile.Crypto();
    crypto.setCipher("aes-128-ctr");
    crypto.setCiphertext(Numeric.toHexStringNoPrefix(cipherText));
    WalletFile.CipherParams cipherParams = new WalletFile.CipherParams();
    cipherParams.setIv(Numeric.toHexStringNoPrefix(iv));
    crypto.setCipherparams(cipherParams);
    crypto.setKdf("scrypt");
    WalletFile.ScryptKdfParams kdfParams = new WalletFile.ScryptKdfParams();
    kdfParams.setDklen(32);
    kdfParams.setN(n);
    kdfParams.setP(p);
    kdfParams.setR(8);
    kdfParams.setSalt(Numeric.toHexStringNoPrefix(salt));
    crypto.setKdfparams(kdfParams);
    crypto.setMac(Numeric.toHexStringNoPrefix(mac));
    walletFile.setCrypto(crypto);
    walletFile.setId(UUID.randomUUID().toString());
    walletFile.setVersion(3);
    return walletFile;
  }

  private byte[] generateRandomBytes(int size) {
    byte[] bytes = new byte[size];
    new SecureRandom().nextBytes(bytes);
    return bytes;
  }

  private byte[] generateMac(byte[] derivedKey, byte[] cipherText) {
    byte[] result = new byte[16 + cipherText.length];
    System.arraycopy(derivedKey, 16, result, 0, 16);
    System.arraycopy(cipherText, 0, result, 16, cipherText.length);
    return Hash.sha3(result);
  }

  private byte[] performCipherOperation(int mode, byte[] iv, byte[] encryptKey, byte[] text)
      throws CipherException {
    try {
      IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
      Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
      SecretKeySpec secretKeySpec = new SecretKeySpec(encryptKey, "AES");
      cipher.init(mode, secretKeySpec, ivParameterSpec);
      return cipher.doFinal(text);
    } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException var7) {
      throw new CipherException("Error performing cipher operation", var7);
    }
  }

  private byte[] generateDerivedScryptKey(byte[] password, byte[] salt, int n, int r, int p,
      int dkLen) {
    return SCrypt.generate(password, salt, n, r, p, dkLen);
  }

  private WalletFile jsonFile2WalletFile(String finalPath) {
    String jsonString;
    try {
      File jsonFile = new File(finalPath);
      FileReader fileReader = new FileReader(jsonFile);
      Reader reader = new InputStreamReader(new FileInputStream(jsonFile), StandardCharsets.UTF_8);
      int ch = 0;
      StringBuilder sb = new StringBuilder();
      while ((ch = reader.read()) != -1) {
        sb.append((char) ch);
      }
      fileReader.close();
      reader.close();
      jsonString = sb.toString();
      JSONObject object = new JSONObject(jsonString);
      String address = object.getString("address");
      String id = object.getString("id");
      int version = object.getInt("version");
      JSONObject crypto = object.getJSONObject("crypto");
      String cipher = crypto.getString("cipher");
      JSONObject cipherparams = crypto.getJSONObject("cipherparams");
      String iv = cipherparams.getString("iv");
      String ciphertext = crypto.getString("ciphertext");
      String kdf = crypto.getString("kdf");
      JSONObject kdfparams = crypto.getJSONObject("kdfparams");
      int dklen = kdfparams.getInt("dklen");
      int n = kdfparams.getInt("n");
      int p = kdfparams.getInt("p");
      int r = kdfparams.getInt("r");
      String salt = kdfparams.getString("salt");
      String mac = crypto.getString("mac");
      WalletFile walletFile = new WalletFile();
      walletFile.setAddress(address);
      walletFile.setId(id);
      walletFile.setVersion(version);
      WalletFile.Crypto wCrypto = new WalletFile.Crypto();
      wCrypto.setCipher(cipher);
      WalletFile.CipherParams parameters = new WalletFile.CipherParams();
      parameters.setIv(iv);
      wCrypto.setCipherparams(parameters);
      wCrypto.setKdf(kdf);
      wCrypto.setCiphertext(ciphertext);
      wCrypto.setKdf(kdf);
      WalletFile.ScryptKdfParams kdfParams = new WalletFile.ScryptKdfParams();
      kdfParams.setDklen(dklen);
      kdfParams.setN(n);
      kdfParams.setP(p);
      kdfParams.setR(r);
      kdfParams.setSalt(salt);
      wCrypto.setKdfparams(kdfParams);
      wCrypto.setMac(mac);
      walletFile.setCrypto(wCrypto);
      return walletFile;
    } catch (Exception e) {
      Log.e(TAG, e.getMessage());
      return null;
    }
  }

  private String getPrivateKey(ECKeyPair eCKeyPair) {
    byte[] array = eCKeyPair.getPrivateKey().toByteArray();
    if (array[0] == 0) {
      byte[] tmp = new byte[array.length - 1];
      System.arraycopy(array, 1, tmp, 0, tmp.length);
      array = tmp;
    }
    return new String(array);
  }
}

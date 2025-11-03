import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;
import okio.ByteString;
import java.io.IOException;
import java.util.logging.Logger;

public final class ByteStrings {


  private static final Logger logger = Logger.getLogger(ByteStrings.class.getName());

  public void run() throws Exception {

    String json = "\"TW9zaGksIE9saXZlLCBXaGl0ZSBDaGluPw\"";

    Moshi moshi = new Moshi.Builder().add(ByteString.class, new Base64ByteStringAdapter()).build();

    JsonAdapter<ByteString> jsonAdapter = moshi.adapter(ByteString.class);

    ByteString byteString = jsonAdapter.fromJson(json);

    logger.info(byteString.toString());
  }

  /**
   * Formats byte strings using <a href="http://www.ietf.org/rfc/rfc2045.txt">Base64</a>. No line
   * breaks or whitespace is included in the encoded form.
   */
  public final class Base64ByteStringAdapter extends JsonAdapter<ByteString> {
    @Override
    public ByteString fromJson(JsonReader reader) throws IOException {
      String base64 = reader.nextString();
      return ByteString.decodeBase64(base64);
    }

    @Override
    public void toJson(JsonWriter writer, ByteString value) throws IOException {
      String string = value.base64();
      writer.value(string);
    }
  }

  public static void main(String[] args) throws Exception {
    new ByteStrings().run();
  }
}

package simpletimers.rs;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("message")
public class MessageResource {

    @GET
    public String whatever() {
        return "hey!";
    }
}
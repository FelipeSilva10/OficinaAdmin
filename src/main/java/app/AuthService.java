package app;

public class AuthService {

    public boolean authenticate(String user, String pass) {
        return user.equals("admin") && pass.equals("admin");
    }
}
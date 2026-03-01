package core;

public class Sessao {
    private static Usuario usuarioLogado;

    public static void login(Usuario u) {
        usuarioLogado = u;
    }

    public static Usuario getUsuario() {
        return usuarioLogado;
    }

    public static void logout() {
        usuarioLogado = null;
    }
}

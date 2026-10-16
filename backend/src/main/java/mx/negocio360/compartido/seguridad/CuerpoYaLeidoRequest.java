package mx.negocio360.compartido.seguridad;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * La peticion con su cuerpo ya leido (y ya acotado), para que el resto de la cadena lo lea igual que el original.
 * Anuncia la longitud real, no la que dijo el cliente.
 */
final class CuerpoYaLeidoRequest extends HttpServletRequestWrapper {

    private final byte[] cuerpo;

    CuerpoYaLeidoRequest(HttpServletRequest peticion, byte[] cuerpo) {
        super(peticion);
        this.cuerpo = cuerpo;
    }

    @Override
    public ServletInputStream getInputStream() {
        var origen = new ByteArrayInputStream(cuerpo);
        return new ServletInputStream() {
            @Override
            public int read() {
                return origen.read();
            }

            @Override
            public int read(byte[] destino, int desde, int largo) {
                return origen.read(destino, desde, largo);
            }

            @Override
            public boolean isFinished() {
                return origen.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener oyente) {
                throw new IllegalStateException("La lectura asincrona del cuerpo no esta disponible");
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        var codificacion = getCharacterEncoding();
        var juego = codificacion == null ? StandardCharsets.UTF_8 : Charset.forName(codificacion);
        return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(cuerpo), juego));
    }

    @Override
    public int getContentLength() {
        return cuerpo.length;
    }

    @Override
    public long getContentLengthLong() {
        return cuerpo.length;
    }
}

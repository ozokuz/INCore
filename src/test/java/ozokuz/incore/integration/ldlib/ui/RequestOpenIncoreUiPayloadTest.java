package ozokuz.incore.integration.ldlib.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

class RequestOpenIncoreUiPayloadTest {
    @Test
    void streamCodecRoundTripsRouteId() {
        RequestOpenIncoreUiPayload payload = new RequestOpenIncoreUiPayload(INCoreUiIds.PLAYER_STATUS);
        var buffer = Unpooled.buffer();

        RequestOpenIncoreUiPayload.STREAM_CODEC.encode(buffer, payload);
        RequestOpenIncoreUiPayload decoded = RequestOpenIncoreUiPayload.STREAM_CODEC.decode(buffer);

        assertEquals(payload.routeId(), decoded.routeId());
    }
}

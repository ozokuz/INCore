package ozokuz.incore.integration.ldlib.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

class RequestPushIncoreUiPayloadTest {
    @Test
    void streamCodecRoundTripsRouteId() {
        RequestPushIncoreUiPayload payload = new RequestPushIncoreUiPayload(INCoreUiIds.GACHA_INFO);
        var buffer = Unpooled.buffer();

        RequestPushIncoreUiPayload.STREAM_CODEC.encode(buffer, payload);
        RequestPushIncoreUiPayload decoded = RequestPushIncoreUiPayload.STREAM_CODEC.decode(buffer);

        assertEquals(payload.routeId(), decoded.routeId());
    }
}

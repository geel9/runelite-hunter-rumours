package com.geel.hunterrumours;

import com.geel.hunterrumours.enums.Hunter;
import com.geel.hunterrumours.enums.Rumour;
import com.google.inject.Guice;
import com.google.inject.testing.fieldbinder.Bind;
import com.google.inject.testing.fieldbinder.BoundFieldModule;
import net.runelite.api.Client;
import net.runelite.api.NPCComposition;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.inject.Inject;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ChatParserTest {
    @Mock
    @Bind
    private Client client;

    @Inject
    private ChatParser chatParser;

    private static String wolfHunterName = "Guild Hunter Wolf (Master)";

    @Before
    public void setUp() {
        Guice.createInjector(BoundFieldModule.of(this)).injectMembers(this);

        for (Hunter hunter : Hunter.values()) {
            NPCComposition mockHunter = mock(NPCComposition.class);
            // We're using common name as a placeholder since we'll always get the 'real' name anyways
            when(mockHunter.getName()).thenReturn(hunter.getCommonName());
            when(client.getNpcDefinition(hunter.getNpcId())).thenReturn(mockHunter);
        }
    }

    @Test
    public void chatParserTests() {
        if (chatParser.getSpeakingHunter(Hunter.MASTER_WOLF.getCommonName() + "| im testing") != Hunter.MASTER_WOLF) {
            fail("You messed up the getSpeakingHunter method of ChatParser");
        }

        if (chatParser.getReferencedRumour("your next assignment is aaa jerboa") != Rumour.EMBERTAILED_JERBOA) {
            fail("You messed up the getReferencedRumour method of ChatParser");
        }

        if (chatParser.getReferencedHunter("Your hunter is " + Hunter.MASTER_WOLF.getCommonName() + " testing") != Hunter.MASTER_WOLF) {
            fail("You messed up the getReferencedHunter method of ChatParser");
        }
    }
}

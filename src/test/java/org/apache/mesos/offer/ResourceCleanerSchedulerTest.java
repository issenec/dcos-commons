package org.apache.mesos.offer;

import org.apache.mesos.Protos;
import org.apache.mesos.SchedulerDriver;
import org.apache.mesos.protobuf.ResourceBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

public class ResourceCleanerSchedulerTest {
    @Mock
    private ResourceCleaner resourceCleaner;
    @Mock
    private OfferAccepter offerAccepter;
    @Mock
    private SchedulerDriver driver;

    private ResourceCleanerScheduler scheduler;
    private List<OfferRecommendation> recommendations;
    private List<Protos.Offer> offers;

    @Before
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
        scheduler = new ResourceCleanerScheduler(resourceCleaner, offerAccepter);
        Protos.Offer offerA = Protos.Offer.newBuilder(Protos.Offer.getDefaultInstance())
                .setHostname("A")
                .setSlaveId(Protos.SlaveID.newBuilder().setValue("A").build())
                .setId(Protos.OfferID.newBuilder().setValue("A"))
                .setFrameworkId(Protos.FrameworkID.newBuilder().setValue("A"))
                .build();
        Protos.Offer offerB = Protos.Offer.newBuilder(Protos.Offer.getDefaultInstance())
                .setHostname("B")
                .setSlaveId(Protos.SlaveID.newBuilder().setValue("B").build())
                .setId(Protos.OfferID.newBuilder().setValue("B"))
                .setFrameworkId(Protos.FrameworkID.newBuilder().setValue("B"))
                .build();

        offers = Arrays.asList(offerA, offerB);

        final DestroyOfferRecommendation destroyRecommendationA = new DestroyOfferRecommendation(offerA, ResourceBuilder.cpus(1));
        final DestroyOfferRecommendation destroyRecommendationB = new DestroyOfferRecommendation(offerB, ResourceBuilder.cpus(1));
        final UnreserveOfferRecommendation unreserveRecommendationA = new UnreserveOfferRecommendation(offerA, ResourceBuilder.cpus(1));
        final UnreserveOfferRecommendation unreserveRecommendationB = new UnreserveOfferRecommendation(offerB, ResourceBuilder.cpus(1));

        recommendations = Arrays.asList(
                destroyRecommendationA,
                destroyRecommendationB,
                unreserveRecommendationA,
                unreserveRecommendationB);
        when(resourceCleaner.evaluate(offers)).thenReturn(recommendations);
    }

    @Test
    public void testResourceOffers() {
        scheduler.resourceOffers(driver, offers);
        verify(offerAccepter, times(2)).accept(any(), any());
    }

    @Test
    public void testGroupRecommendationsByAgent() {
        final Map<Protos.SlaveID, List<OfferRecommendation>> group
                = scheduler.groupRecommendationsByAgent(recommendations);
        Assert.assertTrue(group.size() == 2);
        Protos.SlaveID prevSlaveID = null;
        for (Map.Entry<Protos.SlaveID, List<OfferRecommendation>> entry : group.entrySet()) {
            final List<OfferRecommendation> recommendations = entry.getValue();
            Assert.assertNotNull(recommendations);
            Assert.assertTrue(recommendations.size() == 2);
            final Protos.SlaveID key = entry.getKey();
            Assert.assertEquals(key, recommendations.get(0).getOffer().getSlaveId());
            Assert.assertEquals(key, recommendations.get(1).getOffer().getSlaveId());

            if (prevSlaveID != null) {
                Assert.assertNotEquals(key, prevSlaveID);
            }

            prevSlaveID = key;
        }
    }
}

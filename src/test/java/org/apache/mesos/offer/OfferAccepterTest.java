package org.apache.mesos.offer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.mesos.protobuf.OfferBuilder;
import org.apache.mesos.protobuf.ResourceBuilder;
import org.apache.mesos.protobuf.TaskInfoBuilder;

import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Protos.Offer.Operation;
import org.apache.mesos.Protos.OfferID;
import org.apache.mesos.Protos.Resource;
import org.apache.mesos.Protos.TaskInfo;
import org.apache.mesos.SchedulerDriver;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;

public class OfferAccepterTest {

  @Mock
  private SchedulerDriver driver;

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testConstructor() {
    OfferAccepter accepter = new OfferAccepter(new TestOperationRecorder());
    Assert.assertNotNull(accepter);
  }

  @Test
  public void testLaunchTransient() {
    Resource resource = ResourceBuilder.cpus(1.0);
    Offer offer = getOffer(resource);
    TaskInfo taskInfo = getTaskInfo(resource);
    taskInfo = TaskUtils.setTransient(taskInfo);

    TestOperationRecorder recorder = new TestOperationRecorder();
    OfferAccepter accepter = new OfferAccepter(recorder);
    accepter.accept(driver, Arrays.asList(new LaunchOfferRecommendation(offer, taskInfo)));
    Assert.assertEquals(1, recorder.getLaunches().size());
    verify(driver, times(0)).acceptOffers(
        anyCollectionOf(OfferID.class),
        anyCollectionOf(Operation.class),
        anyObject());
  }

  @Test
  public void testClearTransient() {
    Resource resource = ResourceBuilder.cpus(1.0);
    Offer offer = getOffer(resource);
    TaskInfo taskInfo = getTaskInfo(resource);
    taskInfo = TaskUtils.setTransient(taskInfo);

    TestOperationRecorder recorder = new TestOperationRecorder();
    OfferAccepter accepter = new OfferAccepter(recorder);
    accepter.accept(driver, Arrays.asList(new LaunchOfferRecommendation(offer, taskInfo)));
    Assert.assertEquals(1, recorder.getLaunches().size());
    verify(driver, times(0)).acceptOffers(
        anyCollectionOf(OfferID.class),
        anyCollectionOf(Operation.class),
        anyObject());

    taskInfo = TaskUtils.clearTransient(taskInfo);
    accepter.accept(driver, Arrays.asList(new LaunchOfferRecommendation(offer, taskInfo)));
    Assert.assertEquals(2, recorder.getLaunches().size());
    verify(driver, times(1)).acceptOffers(
        anyCollectionOf(OfferID.class),
        anyCollectionOf(Operation.class),
        anyObject());
  }

  private List<Offer> getOffers(List<Resource> resources) {
    OfferBuilder builder = new OfferBuilder(
        ResourceTestUtils.testOfferId,
        ResourceTestUtils.testFrameworkId,
        ResourceTestUtils.testSlaveId,
        ResourceTestUtils.testHostname);
    builder.addAllResources(resources);
    return Arrays.asList(builder.build());
  }

  private List<Offer> getOffers(Resource resource) {
    return getOffers(Arrays.asList(resource));
  }

  private Offer getOffer(Resource resource) {
    return getOffers(resource).get(0);
  }

  private TaskInfo getTaskInfo(Resource resource) {
    return getTaskInfo(Arrays.asList(resource));
  }

  private TaskInfo getTaskInfo(List<Resource> resources) {
    TaskInfoBuilder builder = new TaskInfoBuilder(
        ResourceTestUtils.testTaskId,
        ResourceTestUtils.testTaskName,
        ResourceTestUtils.testSlaveId);

    return builder.addAllResources(resources).build();
  }

  public static class TestOperationRecorder implements OperationRecorder {
    private List<Operation> reserves = new ArrayList<Operation>();
    private List<Operation> unreserves = new ArrayList<Operation>();
    private List<Operation> creates = new ArrayList<Operation>();
    private List<Operation> destroys = new ArrayList<Operation>();
    private List<Operation> launches = new ArrayList<Operation>();

    public void record(Operation operation, Offer offer) throws Exception {
      switch (operation.getType()) {
        case UNRESERVE:
          unreserves.add(operation);
          break;
        case RESERVE:
          reserves.add(operation);
          break;
        case CREATE:
          creates.add(operation);
          break;
        case DESTROY:
          destroys.add(operation);
          break;
        case LAUNCH:
          launches.add(operation);
          break;
        default:
          throw new Exception("Unknown operation type encountered");
      }
    }

    public List<Operation> getReserves() {
      return reserves;
    }

    public List<Operation> getUnreserves() {
      return unreserves;
    }

    public List<Operation> getCreates() {
      return creates;
    }

    public List<Operation> getDestroys() {
      return destroys;
    }

    public List<Operation> getLaunches() {
      return launches;
    }
  }
}

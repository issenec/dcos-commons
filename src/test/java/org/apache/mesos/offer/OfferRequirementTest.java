package org.apache.mesos.offer;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.mesos.Protos.CommandInfo;
import org.apache.mesos.Protos.ExecutorInfo;
import org.apache.mesos.Protos.Resource;
import org.apache.mesos.Protos.TaskInfo;

import org.apache.mesos.protobuf.ExecutorInfoBuilder;
import org.apache.mesos.protobuf.ResourceBuilder;
import org.apache.mesos.protobuf.TaskInfoBuilder;

import org.junit.Assert;
import org.junit.Test;

public class OfferRequirementTest {

  @Test
  public void testConstructor() throws InvalidRequirementException {
    Resource resource = ResourceBuilder.cpus(1.0);
    OfferRequirement offerRequirement = getOfferRequirement(resource);
    Assert.assertNotNull(offerRequirement);
  }

  @Test
  public void testNoIds() throws InvalidRequirementException {
    Resource resource = ResourceBuilder.cpus(1.0);
    OfferRequirement offerRequirement = getOfferRequirement(resource);
    Assert.assertEquals(0, offerRequirement.getResourceIds().size());
  }

  @Test
  public void testOneResourceId() throws InvalidRequirementException {
    String testResourceId = UUID.randomUUID().toString();
    Resource resource = ResourceBuilder.reservedCpus(1.0, ResourceTestUtils.testRole, ResourceTestUtils.testPrincipal, testResourceId);
    OfferRequirement offerRequirement = getOfferRequirement(resource);
    Assert.assertEquals(1, offerRequirement.getResourceIds().size());
    Assert.assertEquals(testResourceId, offerRequirement.getResourceIds().iterator().next());
  }

  @Test
  public void testOnePersistenceId() throws InvalidRequirementException {
    Resource resource = ResourceBuilder.volume(1000.0, ResourceTestUtils.testRole, ResourceTestUtils.testPrincipal, ResourceTestUtils.testContainerPath, ResourceTestUtils.testPersistenceId);
    OfferRequirement offerRequirement = getOfferRequirement(resource);
    Assert.assertEquals(1, offerRequirement.getResourceIds().size());
    Assert.assertTrue(offerRequirement.getResourceIds().contains(ResourceTestUtils.testPersistenceId));
    Assert.assertEquals(1, offerRequirement.getPersistenceIds().size());
    Assert.assertEquals(ResourceTestUtils.testPersistenceId, offerRequirement.getPersistenceIds().iterator().next());
  }

  @Test
  public void testOneOfEachId() throws InvalidRequirementException {
    String testResourceId = UUID.randomUUID().toString();
    Resource cpu = ResourceBuilder.reservedCpus(1.0, ResourceTestUtils.testRole, ResourceTestUtils.testPrincipal, testResourceId);
    Resource volume = ResourceBuilder.volume(1000.0, ResourceTestUtils.testRole, ResourceTestUtils.testPrincipal, ResourceTestUtils.testContainerPath, ResourceTestUtils.testPersistenceId);
    OfferRequirement offerRequirement = getOfferRequirement(Arrays.asList(cpu, volume));
    Assert.assertEquals(2, offerRequirement.getResourceIds().size());
    Assert.assertTrue(testResourceId, offerRequirement.getResourceIds().contains(testResourceId));
    Assert.assertTrue(ResourceTestUtils.testPersistenceId, offerRequirement.getResourceIds().contains(testResourceId));
    Assert.assertEquals(1, offerRequirement.getPersistenceIds().size());
    Assert.assertEquals(ResourceTestUtils.testPersistenceId, offerRequirement.getPersistenceIds().iterator().next());
  }

  @Test
  public void testExecutor() throws InvalidRequirementException {
    Resource cpu = ResourceBuilder.reservedCpus(1.0, ResourceTestUtils.testRole, ResourceTestUtils.testPrincipal, UUID.randomUUID().toString());
    TaskInfo taskInfo = getTaskInfo(cpu);
    ExecutorInfo execInfo = getExecutorInfo(cpu);
    OfferRequirement offerRequirement = new OfferRequirement(Arrays.asList(taskInfo), execInfo);
    Resource executorResource = offerRequirement
        .getExecutorRequirement()
        .getExecutorInfo()
        .getResourcesList()
        .get(0);

    Assert.assertEquals(cpu, executorResource);
  }

  private OfferRequirement getOfferRequirement(Resource resource) throws InvalidRequirementException {
    return getOfferRequirement(Arrays.asList(resource));
  }

  private OfferRequirement getOfferRequirement(List<Resource> resources) throws InvalidRequirementException {
    return new OfferRequirement(Arrays.asList(getTaskInfo(resources)));
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

  private ExecutorInfo getExecutorInfo(Resource resource) {
    return getExecutorInfo(Arrays.asList(resource));
  }

  private ExecutorInfo getExecutorInfo(List<Resource> resources) {
    CommandInfo cmd = CommandInfo.newBuilder().build();
    ExecutorInfoBuilder builder = new ExecutorInfoBuilder(
        ResourceTestUtils.testExecutorId, ResourceTestUtils.testExecutorName, cmd);
    return builder.addAllResources(resources).build();
  }
}

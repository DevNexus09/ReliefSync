package com.reliefsync.application;

import com.reliefsync.controller.*;
import java.util.Objects;
import javafx.util.Callback;

public final class ControllerFactory implements Callback<Class<?>, Object> {
  private final ApplicationContext context;

  public ControllerFactory(ApplicationContext context) {
    this.context = Objects.requireNonNull(context);
  }

  @Override
  public Object call(Class<?> controllerType) {
    if (controllerType == LoginController.class) {
      return new LoginController(context.authenticationService());
    }
    if (controllerType == DashboardController.class) {
      return new DashboardController(
          context.authenticationService(),
          context.sessionManager(),
          context.authorizationService());
    }
    if (controllerType == DisasterEventController.class)
      return new DisasterEventController(
          context.disasterEventService(), context.sessionManager(), context.authorizationService());
    if (controllerType == AffectedAreaController.class)
      return new AffectedAreaController(
          context.affectedAreaService(), context.sessionManager(), context.authorizationService());
    if (controllerType == ReliefCenterController.class)
      return new ReliefCenterController(
          context.reliefCenterService(), context.sessionManager(), context.authorizationService());
    if (controllerType == ResourceController.class)
      return new ResourceController(
          context.resourceService(), context.sessionManager(), context.authorizationService());
    if (controllerType == InventoryController.class)
      return new InventoryController(
          context.inventoryService(), context.sessionManager(), context.authorizationService());
    if (controllerType == VehicleController.class)
      return new VehicleController(
          context.vehicleService(), context.sessionManager(), context.authorizationService());
    throw new IllegalArgumentException("Unsupported FXML controller: " + controllerType.getName());
  }
}

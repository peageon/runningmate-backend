package com.runningmate.backend.route.service;

import com.runningmate.backend.exception.BadRequestException;
import com.runningmate.backend.exception.ResourceNotFoundException;
import com.runningmate.backend.member.dto.MemberDto;
import com.runningmate.backend.member.service.MemberService;
import com.runningmate.backend.route.Route;
import com.runningmate.backend.route.dto.CoordinateDto;
import com.runningmate.backend.route.dto.RouteRequestDto;
import com.runningmate.backend.route.dto.RouteResponseDto;
import com.runningmate.backend.route.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RouteService {
    private final RouteRepository routeRepository;
    private final MemberService memberService;

    @Transactional
    public RouteResponseDto saveRoute(RouteRequestDto request, String username) {
        List<CoordinateDto> coordinateDtos = request.getRoute();
        validateCoordinates(coordinateDtos);
        LineString lineString = coordinateDtoListToLineString(coordinateDtos);
        Route route = request.toEntity(memberService.getMemberByUsername(username), lineString);
        routeRepository.save(route);
        return new RouteResponseDto(route, coordinateDtos);
    }

    public RouteResponseDto getRouteById(Long routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Course with id " + routeId + " does not exist"));
        List<CoordinateDto> translatedCoordinates = lineStringToCoordinateDtoList(route.getPath());
        return new RouteResponseDto(route, translatedCoordinates);
    }

    public List<RouteResponseDto> getRoutesWithinRadius(double latitude, double longitude, int radius) {
        validateCoordinate(new CoordinateDto(latitude, longitude));
        List<Route> routes = routeRepository.findRoutesWithinRadius(latitude, longitude, radius);
        List<RouteResponseDto> routeDtos = new ArrayList<>();
        for (Route route: routes) {
            List<CoordinateDto> translatedCoordinates = lineStringToCoordinateDtoList(route.getPath());
            routeDtos.add(new RouteResponseDto(route, translatedCoordinates));
        }
        return routeDtos;
    }

    private LineString coordinateDtoListToLineString(List<CoordinateDto> coordinateDtos) {
        GeometryFactory geometryFactory = new GeometryFactory();
        Coordinate[] coordinates = coordinateDtos.stream()
                .map(dto -> new Coordinate(dto.getLongitude(), dto.getLatitude()))
                .toArray(Coordinate[]::new);
        LineString lineString = geometryFactory.createLineString(coordinates);
        return lineString;
    }

    private List<CoordinateDto> lineStringToCoordinateDtoList(LineString lineString) {
        List<CoordinateDto> coordinateDtos = new ArrayList<>();
        for (Coordinate coordinate : lineString.getCoordinates()) {
            CoordinateDto dto = new CoordinateDto();
            dto.setLatitude(coordinate.getY());
            dto.setLongitude(coordinate.getX());
            coordinateDtos.add(dto);
        }
        return coordinateDtos;
    }

    private void validateCoordinate(CoordinateDto coordinateDto) {
        if (coordinateDto.getLatitude() < -90 || coordinateDto.getLatitude() > 90) {
            throw new BadRequestException("Invalid latitude: " + coordinateDto.getLatitude() + "\n Latitude must be within -90 and 90");
        }
        if (coordinateDto.getLongitude() < -180 || coordinateDto.getLongitude() > 180) {
            throw new BadRequestException("Invalid longitude: " + coordinateDto.getLongitude() + "\n Longitude must be within -180 and 180");
        }
    }

    private void validateCoordinates(List<CoordinateDto> coordinateDTOs) {
        for (CoordinateDto dto : coordinateDTOs) {
            validateCoordinate(dto);
        }
    }
}

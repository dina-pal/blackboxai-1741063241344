package com.dinapal.busdakho.data.mapper

/**
 * Base interface for mapping between different data models
 * @param Model The domain model type
 * @param Entity The database entity type
 * @param Dto The data transfer object type (API response/request)
 */
interface BaseMapper<Model, Entity, Dto> {
    /**
     * Maps a domain model to a database entity
     */
    fun mapToEntity(model: Model): Entity

    /**
     * Maps a database entity to a domain model
     */
    fun mapFromEntity(entity: Entity): Model

    /**
     * Maps a data transfer object to a domain model
     */
    fun mapFromDto(dto: Dto): Model

    /**
     * Maps a domain model to a data transfer object
     */
    fun mapToDto(model: Model): Dto

    /**
     * Maps a list of domain models to a list of database entities
     */
    fun mapToEntityList(models: List<Model>): List<Entity> {
        return models.map { mapToEntity(it) }
    }

    /**
     * Maps a list of database entities to a list of domain models
     */
    fun mapFromEntityList(entities: List<Entity>): List<Model> {
        return entities.map { mapFromEntity(it) }
    }

    /**
     * Maps a list of data transfer objects to a list of domain models
     */
    fun mapFromDtoList(dtos: List<Dto>): List<Model> {
        return dtos.map { mapFromDto(it) }
    }

    /**
     * Maps a list of domain models to a list of data transfer objects
     */
    fun mapToDtoList(models: List<Model>): List<Dto> {
        return models.map { mapToDto(it) }
    }
}

/**
 * Interface for mapping between domain models and database entities
 */
interface EntityMapper<Model, Entity> {
    fun mapToEntity(model: Model): Entity
    fun mapFromEntity(entity: Entity): Model
    fun mapToEntityList(models: List<Model>): List<Entity> = models.map { mapToEntity(it) }
    fun mapFromEntityList(entities: List<Entity>): List<Model> = entities.map { mapFromEntity(it) }
}

/**
 * Interface for mapping between domain models and DTOs
 */
interface DtoMapper<Model, Dto> {
    fun mapFromDto(dto: Dto): Model
    fun mapToDto(model: Model): Dto
    fun mapFromDtoList(dtos: List<Dto>): List<Model> = dtos.map { mapFromDto(it) }
    fun mapToDtoList(models: List<Model>): List<Dto> = models.map { mapToDto(it) }
}

/**
 * Interface for mapping between two different types
 */
interface Mapper<I, O> {
    fun map(input: I): O
    fun mapList(input: List<I>): List<O> = input.map { map(it) }
}

/**
 * Interface for bi-directional mapping between two types
 */
interface BiDirectionalMapper<A, B> {
    fun mapToB(a: A): B
    fun mapToA(b: B): B
    fun mapToBList(aList: List<A>): List<B> = aList.map { mapToB(it) }
    fun mapToAList(bList: List<B>): List<B> = bList.map { mapToA(it) }
}

/**
 * Abstract class for mapping between nullable types with default values
 */
abstract class NullableMapper<I, O> : Mapper<I?, O> {
    abstract fun mapNonNull(input: I): O
    abstract fun defaultValue(): O

    override fun map(input: I?): O {
        return input?.let { mapNonNull(it) } ?: defaultValue()
    }
}

/**
 * Extension functions for mapping operations
 */
fun <I, O> List<I>.mapWith(mapper: Mapper<I, O>): List<O> {
    return map { mapper.map(it) }
}

fun <I, O> List<I>?.mapWithOrEmpty(mapper: Mapper<I, O>): List<O> {
    return this?.mapWith(mapper) ?: emptyList()
}

fun <A, B> List<A>.mapBidirectional(mapper: BiDirectionalMapper<A, B>): List<B> {
    return map { mapper.mapToB(it) }
}

/**
 * Utility class for combining multiple mappers
 */
class CompositeMapper<A, B, C>(
    private val firstMapper: Mapper<A, B>,
    private val secondMapper: Mapper<B, C>
) : Mapper<A, C> {
    override fun map(input: A): C {
        return secondMapper.map(firstMapper.map(input))
    }
}

/**
 * Extension function to chain mappers
 */
infix fun <A, B, C> Mapper<A, B>.then(other: Mapper<B, C>): Mapper<A, C> {
    return CompositeMapper(this, other)
}

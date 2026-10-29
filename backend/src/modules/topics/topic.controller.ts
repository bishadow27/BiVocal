import type { RequestHandler } from 'express';
import type { TopicService } from './topic.service.js';

export function createTopicController(service: TopicService): { list: RequestHandler; catalog: RequestHandler } {
  return {
    async list(_request, response) {
      response.json(await service.list());
    },
    async catalog(_request, response) {
      response.json(await service.catalog());
    },
  };
}

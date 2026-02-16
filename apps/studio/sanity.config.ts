import {defineConfig} from 'sanity'
import {structureTool} from 'sanity/structure'
import {visionTool} from '@sanity/vision'
import {schemaTypes} from './schemaTypes'

export default defineConfig({
  name: 'default',
  title: 'juuffs',

  projectId: 'jy1r6rh3',
  dataset: 'production',

  plugins: [structureTool({
    title: 'my names',

  }), visionTool()],

  schema: {
    types: schemaTypes,
  },
})
